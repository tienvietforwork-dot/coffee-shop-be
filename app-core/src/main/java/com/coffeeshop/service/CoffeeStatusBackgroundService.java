package com.coffeeshop.service;

import com.coffeeshop.entity.BackgroundServiceLog;
import com.coffeeshop.repository.BackgroundServiceLogRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Background service that keeps coffee status in step with stock (StockService.syncCoffeeStatus):
 * runs every cycle (app.inventory.status-scan-ms, 5 minutes), and anything that changes stock,
 * recipes or a coffee's status calls {@link #requestRun()} instead of having its own logic.
 * Every run is written to background_service_logs with its duration — same pattern as MES's
 * ProductionStatusBackgroundService / A_BACKGROUND_SERVICE.
 */
@Service
@Slf4j
public class CoffeeStatusBackgroundService {

    public static final String NAME = "CoffeeStatusBackgroundService";
    private static final String SCHEDULER = "Service";

    private final StockService stockService;
    private final BackgroundServiceLogRepository logRepository;
    /** runs triggered by users, off the request thread */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "coffee-status-bg");
        t.setDaemon(true);
        return t;
    });
    private final long cycleMs;
    /** one run at a time: the timer and user-triggered runs queue up behind each other */
    private final ReentrantLock lock = new ReentrantLock();

    public CoffeeStatusBackgroundService(StockService stockService, BackgroundServiceLogRepository logRepository,
                                         @Value("${app.inventory.status-scan-ms:300000}") long cycleMs) {
        this.stockService = stockService;
        this.logRepository = logRepository;
        this.cycleMs = cycleMs;
    }

    @Scheduled(fixedDelayString = "${app.inventory.status-scan-ms:300000}", initialDelay = 15 * 1000L)
    public void runOnSchedule() {
        call(SCHEDULER);
    }

    /**
     * Run once the caller's transaction has committed (so the scan sees the new stock), on a worker
     * thread so the request does not wait for it.
     */
    public void requestRun() {
        String user = CurrentUser.username();
        Runnable run = () -> worker.execute(() -> call(user));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                run.run();
            }
        });
    }

    /** One scan; returns the message that was logged. */
    public String call(String triggeredBy) {
        lock.lock();
        LocalDateTime checkTime = LocalDateTime.now();
        long start = System.nanoTime();
        String msg;
        try {
            var changes = stockService.syncCoffeeStatus();
            msg = changes.isEmpty() ? "Không đổi trạng thái món nào"
                    : changes.stream().map(c -> c.name() + " → " + c.status()).collect(Collectors.joining(", "));
        } catch (Exception e) {
            msg = "Error: " + e.getMessage();
            log.error("{} failed", NAME, e);
        } finally {
            lock.unlock();
        }
        int ms = (int) ((System.nanoTime() - start) / 1_000_000);
        log.info("{} ({}) {} ms: {}", NAME, triggeredBy, ms, msg);
        try {
            BackgroundServiceLog row = BackgroundServiceLog.builder()
                    .service(NAME)
                    .checkTime(checkTime)
                    .durationMs(ms)
                    .msg(msg)
                    // only the timer knows when it runs next
                    .nextRunTime(SCHEDULER.equals(triggeredBy) ? LocalDateTime.now().plusNanos(cycleMs * 1_000_000) : null)
                    .build();
            row.setCreatedBy(triggeredBy);
            logRepository.save(row);
        } catch (Exception e) {
            log.error("Could not write {} log", NAME, e);
        }
        return msg;
    }
}

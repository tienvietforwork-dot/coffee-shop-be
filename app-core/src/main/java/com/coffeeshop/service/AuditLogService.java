package com.coffeeshop.service;

import com.coffeeshop.entity.AuditLog;
import com.coffeeshop.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Nhật ký thao tác. Requests are put on an in-memory queue by the web filter (so a request never waits on the log)
 * and a job writes them in batches every few seconds; another job drops entries older than the retention.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int BATCH = 500;

    private final AuditLogRepository repository;
    private final BlockingQueue<AuditLog> queue = new ArrayBlockingQueue<>(10_000);

    @Value("${app.audit.retention-days:180}")
    private int retentionDays;

    /** from the web filter; dropped (with a warning) if the queue is full */
    public void enqueue(AuditLog entry) {
        if (!queue.offer(entry)) log.warn("Audit queue full, dropped {} {}", entry.getMethod(), entry.getPath());
    }

    @Scheduled(fixedDelayString = "${app.audit.flush-ms:3000}")
    @Transactional
    public int flush() {
        List<AuditLog> batch = new ArrayList<>(BATCH);
        int total = 0;
        while (queue.drainTo(batch, BATCH) > 0) {
            repository.saveAll(batch);
            total += batch.size();
            batch.clear();
        }
        return total;
    }

    @Scheduled(cron = "${app.audit.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int cleanup() {
        int n = repository.deleteOlderThan(LocalDate.now().minusDays(retentionDays).atStartOfDay());
        if (n > 0) log.info("Xóa {} dòng nhật ký thao tác cũ hơn {} ngày", n, retentionDays);
        return n;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(LocalDate from, LocalDate to, String username, String module, boolean failed,
                                 String keyword, int page, int size) {
        String kw = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
        return repository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), blank(username), blank(module), failed,
                kw, PageRequest.of(page, Math.min(size, 500)));
    }

    @Transactional(readOnly = true)
    public List<String> usernames() {
        return repository.usernames();
    }

    @Transactional(readOnly = true)
    public List<String> modules() {
        return repository.modules();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** for tests: everything queued so far is written */
    public LocalDateTime flushNow() {
        flush();
        return LocalDateTime.now();
    }
}

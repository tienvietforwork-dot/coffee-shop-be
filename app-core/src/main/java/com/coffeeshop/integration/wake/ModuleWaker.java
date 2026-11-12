package com.coffeeshop.integration.wake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Render free tier puts idle apps to sleep. When app-core wakes up (and on each login) it pings the
 * other modules' health endpoints so app-crm / app-stats / app-promotions are already starting by the
 * time someone opens their pages. Fire-and-forget; empty WAKE_URLS (local) disables it.
 */
@Slf4j
@Component
public class ModuleWaker {

    /** Don't re-ping more often than this (logins can come in bursts). */
    private static final long MIN_INTERVAL_MS = Duration.ofMinutes(5).toMillis();

    private final List<String> urls;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final AtomicLong lastWake = new AtomicLong(0);

    public ModuleWaker(@Value("${app.wake-urls:}") String wakeUrls) {
        this.urls = Arrays.stream(wakeUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("/+$", "") + "/actuator/health")
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        wakeAll();
    }

    public void wakeAll() {
        if (urls.isEmpty()) return;
        long now = System.currentTimeMillis();
        long last = lastWake.get();
        if (now - last < MIN_INTERVAL_MS || !lastWake.compareAndSet(last, now)) return;

        for (String url : urls) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(120)) // cold start on Render can take ~1-2 min
                    .GET()
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((res, err) -> {
                        if (err != null) log.warn("Wake {} failed: {}", url, err.toString());
                        else log.info("Wake {} -> {}", url, res.statusCode());
                    });
        }
    }
}

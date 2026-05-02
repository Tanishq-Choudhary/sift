package com.sift.alerting;

import com.sift.detection.AlertJsonLineLogger;
import com.sift.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncAlertDispatcher implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncAlertDispatcher.class);
    private static final int DEFAULT_CAPACITY = 10_000;

    private final AlertPublisher alertPublisher;
    private final AlertJsonLineLogger alertJsonLineLogger;
    private final BlockingQueue<Alert> queue;
    private final Thread thread;
    private final AtomicLong alertsDropped = new AtomicLong();
    private volatile boolean running = true;

    public AsyncAlertDispatcher(AlertPublisher alertPublisher, AlertJsonLineLogger alertJsonLineLogger) {
        this(alertPublisher, alertJsonLineLogger, DEFAULT_CAPACITY);
    }

    public AsyncAlertDispatcher(AlertPublisher alertPublisher, AlertJsonLineLogger alertJsonLineLogger, int capacity) {
        this.alertPublisher = alertPublisher;
        this.alertJsonLineLogger = alertJsonLineLogger;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.thread = new Thread(this::runLoop, "sift-alert-dispatcher");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void dispatch(Alert alert) {
        if (!running || !queue.offer(alert)) {
            alertsDropped.incrementAndGet();
        }
    }

    private void runLoop() {
        while (running || !queue.isEmpty()) {
            Alert alert;
            try {
                alert = queue.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (alert != null) {
                send(alert);
            }
        }
    }

    private void send(Alert alert) {
        try {
            if (alertPublisher != null) {
                alertPublisher.publish(alert);
            }
            if (alertJsonLineLogger != null) {
                alertJsonLineLogger.append(alert);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to dispatch alert: {}", e.getMessage());
        }
    }

    public long getAlertsDropped() { return alertsDropped.get(); }
    public int getQueueDepth() { return queue.size(); }

    @Override
    public void close() {
        running = false;
        try {
            thread.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

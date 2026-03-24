package com.sift.detection;

import com.sift.model.Alert;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AlertRingBuffer {

    private final int capacity;
    private final Deque<Alert> alerts = new ArrayDeque<>();

    public AlertRingBuffer(int capacity) {
        this.capacity = capacity;
    }

    public void add(Alert alert) {
        if (alerts.size() == capacity) {
            alerts.removeFirst();
        }
        alerts.addLast(alert);
    }

    public List<Alert> recent(int limit) {
        return alerts.stream()
                .skip(Math.max(0, alerts.size() - limit))
                .toList();
    }

    public int size() {
        return alerts.size();
    }
}
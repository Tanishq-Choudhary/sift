package com.sift.api;

import com.sift.detection.AlertRingBuffer;
import com.sift.model.Alert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertController {

    private final AlertRingBuffer alertRingBuffer;

    public AlertController(AlertRingBuffer alertRingBuffer) {
        this.alertRingBuffer = alertRingBuffer;
    }

    @GetMapping("/api/alerts")
    public AlertsResponse getAlerts(@RequestParam(defaultValue = "50") int limit) {
        return new AlertsResponse(alertRingBuffer.recent(limit));
    }

    public record AlertsResponse(List<Alert> alerts) {}
}
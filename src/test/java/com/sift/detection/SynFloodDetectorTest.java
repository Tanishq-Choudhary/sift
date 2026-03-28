package com.sift.detection;

import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.model.Alert;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynFloodDetectorTest {

    private static final TcpFlags SYN = new TcpFlags(true, false, false, false);

    @Test
    void steadyLowRateTrafficDoesNotTriggerAfterWarmup() {
        FlowTrackingService flowTrackingService = new FlowTrackingService(new FlowTable(1000));
        SynFloodDetector detector = new SynFloodDetector(flowTrackingService);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        for (int second = 0; second < 60; second++) {
            Packet packet = synPacket("198.51.100.1", start.plusSeconds(second));
            flowTrackingService.track(packet);
            assertFalse(detector.check(packet).isPresent());
        }
    }

    @Test
    void suddenBurstAfterBaselineTriggersAlert() {
        FlowTrackingService flowTrackingService = new FlowTrackingService(new FlowTable(1000));
        SynFloodDetector detector = new SynFloodDetector(flowTrackingService);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        for (int second = 0; second < 60; second++) {
            Packet packet = synPacket("198.51.100.1", start.plusSeconds(second));
            flowTrackingService.track(packet);
            detector.check(packet);
        }

        boolean floodDetected = false;
        Instant burstStart = start.plusSeconds(60);
        for (int i = 0; i < 200; i++) {
            Packet packet = synPacket("198.51.100.1", burstStart.plusMillis(i * 5L));
            flowTrackingService.track(packet);
            Optional<Alert> alert = detector.check(packet);
            if (alert.isPresent()) {
                floodDetected = true;
                break;
            }
        }

        assertTrue(floodDetected);
    }

    private Packet synPacket(String sourceIp, Instant timestamp) {
        return new Packet(sourceIp, "203.0.113.50", 40000, 80, Protocol.TCP, SYN, 0, timestamp, null);
    }
}
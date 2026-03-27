package com.sift.detection;

import com.sift.model.Alert;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortScanDetectorTest {

    private static final TcpFlags SYN = new TcpFlags(true, false, false, false);

    @Test
    void fewDistinctPortsDoesNotTrigger() {
        PortScanDetector detector = new PortScanDetector();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        for (int port = 1; port <= 5; port++) {
            Packet packet = tcpSynTo(port, start.plusMillis(port * 100L));
            assertFalse(detector.check(packet).isPresent());
        }
    }

    @Test
    void manyDistinctPortsWithinWindowTriggers() {
        PortScanDetector detector = new PortScanDetector();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        boolean scanDetected = false;
        for (int port = 1; port <= 30; port++) {
            Packet packet = tcpSynTo(port, start.plusMillis(port * 100L));
            Optional<Alert> alert = detector.check(packet);
            if (alert.isPresent()) {
                scanDetected = true;
            }
        }

        assertTrue(scanDetected);
    }

    private Packet tcpSynTo(int destPort, Instant timestamp) {
        return new Packet("198.51.100.5", "10.0.0.9", 55000, destPort, Protocol.TCP, SYN, 0, timestamp, null);
    }
}
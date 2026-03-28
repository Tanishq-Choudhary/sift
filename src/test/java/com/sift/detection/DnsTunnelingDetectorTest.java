package com.sift.detection;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnsTunnelingDetectorTest {

    @Test
    void normalDomainQueryDoesNotTrigger() {
        DnsTunnelingDetector detector = new DnsTunnelingDetector();
        Packet packet = dnsQuery("mail.google.com", Instant.parse("2026-01-01T00:00:00Z"));

        assertFalse(detector.check(packet).isPresent());
    }

    @Test
    void highEntropyLongLabelTriggers() {
        DnsTunnelingDetector detector = new DnsTunnelingDetector();
        Packet packet = dnsQuery(
                "q7x9zv2mkw4pldr8h1n6btsyx3ojgfae5c0.tunnel.example.com",
                Instant.parse("2026-01-01T00:00:00Z"));

        assertTrue(detector.check(packet).isPresent());
    }

    private Packet dnsQuery(String queryName, Instant timestamp) {
        return new Packet("10.0.0.15", "8.8.8.8", 40000, 53, Protocol.UDP, null, 64, timestamp, queryName);
    }
}
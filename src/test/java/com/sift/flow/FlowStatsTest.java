package com.sift.flow;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowStatsTest {

    @Test
    void tracksPacketByteAndSynCounts() {
        FlowStats stats = new FlowStats();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        stats.recordPacket(tcpPacket(start, true, 100));
        stats.recordPacket(tcpPacket(start.plusSeconds(1), false, 200));
        stats.recordPacket(tcpPacket(start.plusSeconds(2), false, 50));

        assertEquals(3, stats.getPacketCount());
        assertEquals(350, stats.getByteCount());
        assertEquals(1, stats.getSynCount());
    }

    @Test
    void packetRateReflectsOnlyPacketsInsideTheWindow() {
        FlowStats stats = new FlowStats();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        for (int second = 0; second < 5; second++) {
            stats.recordPacket(tcpPacket(start.plusSeconds(second), false, 10));
        }

        Instant wellPastWindow = start.plusSeconds(30);
        stats.recordPacket(tcpPacket(wellPastWindow, false, 10));

        assertEquals(1.0 / 10.0, stats.getPacketRatePerSecond(wellPastWindow), 1e-9);
    }

    private static Packet tcpPacket(Instant timestamp, boolean syn, int payloadLen) {
        return new Packet(
                "10.0.0.1", "10.0.0.2", 1234, 80, Protocol.TCP,
                new TcpFlags(syn, !syn, false, false),
                payloadLen, timestamp, null);
    }
}
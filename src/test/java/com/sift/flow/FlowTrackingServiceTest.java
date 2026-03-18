package com.sift.flow;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlowTrackingServiceTest {

    @Test
    void tracksPacketsIntoCorrectFlow() {
        FlowTable flowTable = new FlowTable(10);
        FlowTrackingService service = new FlowTrackingService(flowTable);
        Instant now = Instant.now();

        Packet first = new Packet("10.0.0.1", "10.0.0.2", 1234, 80, Protocol.TCP,
                new TcpFlags(true, false, false, false), 0, now, null);
        Packet second = new Packet("10.0.0.1", "10.0.0.2", 1234, 80, Protocol.TCP,
                new TcpFlags(false, true, false, false), 100, now.plusSeconds(1), null);

        service.track(first);
        FlowStats stats = service.track(second);

        assertEquals(2, stats.getPacketCount());
        assertEquals(100, stats.getByteCount());
        assertEquals(1, stats.getSynCount());
    }

    @Test
    void buildsGlobalSynRateStatsPerSourceIp() {
        FlowTable flowTable = new FlowTable(10);
        FlowTrackingService service = new FlowTrackingService(flowTable);
        Instant now = Instant.now();

        Packet syn = new Packet("10.0.0.9", "10.0.0.2", 1111, 80, Protocol.TCP,
                new TcpFlags(true, false, false, false), 0, now, null);

        service.track(syn);

        assertNotNull(service.getSynRateStats("10.0.0.9"));
        assertEquals(1, service.getSynRateStats("10.0.0.9").getCount());
    }
}
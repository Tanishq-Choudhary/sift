package com.sift.loadgen;

import com.sift.detection.DnsTunnelingDetector;
import com.sift.detection.PortScanDetector;
import com.sift.detection.SynFloodDetector;
import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.loadgen.generators.DnsTunnelTrafficGenerator;
import com.sift.loadgen.generators.MixedTrafficGenerator;
import com.sift.loadgen.generators.NormalTrafficGenerator;
import com.sift.loadgen.generators.PortScanTrafficGenerator;
import com.sift.loadgen.generators.SynFloodTrafficGenerator;
import com.sift.model.Alert;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrafficGeneratorsTest {

    private FlowTrackingService flowTrackingService;

    @BeforeEach
    void setUp() {
        flowTrackingService = new FlowTrackingService(new FlowTable(1000));
    }

    @Test
    void testPortScanGeneratorTriggersDetector() {
        PortScanTrafficGenerator gen = new PortScanTrafficGenerator();
        PortScanDetector detector = new PortScanDetector(10, 20);

        Instant now = Instant.now();
        Optional<Alert> alert = Optional.empty();

        for (int i = 0; i < 30; i++) {
            Packet packet = gen.generatePacket(now);
            var result = detector.check(packet);
            if (result.isPresent()) {
                alert = result;
            }
        }

        assertTrue(alert.isPresent(), "Port scan traffic must trigger PortScanDetector");
    }

    @Test
    void testDnsTunnelGeneratorTriggersDetector() {
        DnsTunnelTrafficGenerator gen = new DnsTunnelTrafficGenerator();
        DnsTunnelingDetector detector = new DnsTunnelingDetector();

        Packet packet = gen.generatePacket(Instant.now());
        Optional<Alert> alert = detector.check(packet);

        assertTrue(alert.isPresent(), "DNS Tunnel traffic must trigger DnsTunnelingDetector");
    }

    @Test
    void testSynFloodGeneratorTriggersDetector() {
        SynFloodTrafficGenerator gen = new SynFloodTrafficGenerator();
        SynFloodDetector detector = new SynFloodDetector(flowTrackingService, 3.0);

        Instant now = Instant.now();

        // Send seed packets
        Packet p1 = gen.generatePacket(now);
        flowTrackingService.track(p1);
        detector.check(p1);

        Packet p2 = gen.generatePacket(now);
        flowTrackingService.track(p2);
        detector.check(p2);

        // Flood traffic
        Optional<Alert> alert = Optional.empty();
        for (int i = 0; i < 1000; i++) {
            Packet packet = gen.generatePacket(now);
            flowTrackingService.track(packet);
            var result = detector.check(packet);
            if (result.isPresent()) {
                alert = result;
            }
        }

        assertTrue(alert.isPresent(), "SYN Flood traffic must trigger SynFloodDetector");
    }

    @Test
    void testMixedTrafficDistribution() {
        LoadGeneratorProperties props = new LoadGeneratorProperties();
        props.setProfile(LoadProfile.MIXED);
        props.setNormalPercent(70);
        props.setSynFloodPercent(20);
        props.setPortScanPercent(7);
        props.setDnsTunnelPercent(3);

        MixedTrafficGenerator generator = new MixedTrafficGenerator(
                new NormalTrafficGenerator(),
                new SynFloodTrafficGenerator(),
                new PortScanTrafficGenerator(),
                new DnsTunnelTrafficGenerator(),
                props
        );

        int iterations = 10_000;
        int dnsCount = 0;
        Instant now = Instant.now();

        for (int i = 0; i < iterations; i++) {
            Packet packet = generator.generatePacket(now);
            if (packet.protocol() == Protocol.UDP && packet.dnsQueryName() != null && packet.dnsQueryName().contains("example.com")) {
                dnsCount++;
            }
        }

        assertTrue(dnsCount > 100, "DNS tunnel traffic should be present according to weight (~3%)");
    }
}
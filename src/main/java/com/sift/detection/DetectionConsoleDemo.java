package com.sift.detection;

import com.sift.alerting.AlertPublisher;
import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.model.Alert;
import com.sift.model.Packet;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DetectionConsoleDemo {

    private static final int FLOW_TABLE_CAPACITY = 50_000;
    private static final int ALERT_RING_BUFFER_CAPACITY = 500;

    public static void main(String[] args) throws Exception {
        FlowTrackingService flowTrackingService = new FlowTrackingService(new FlowTable(FLOW_TABLE_CAPACITY));

        try (AlertPublisher alertPublisher = new AlertPublisher("localhost", 5672)) {
            AlertJsonLineLogger alertJsonLineLogger = new AlertJsonLineLogger(Path.of("alerts.jsonl"));

            AnomalyDetectionService detectionService = new AnomalyDetectionService(
                    new SynFloodDetector(flowTrackingService),
                    new PortScanDetector(),
                    new DnsTunnelingDetector(),
                    new AlertRingBuffer(ALERT_RING_BUFFER_CAPACITY),
                    alertPublisher,
                    alertJsonLineLogger);

            Instant start = Instant.now();
            List<Packet> scenario = buildScenario(new SyntheticTrafficGenerator(), start);

            for (Packet packet : scenario) {
                flowTrackingService.track(packet);
                for (Alert alert : detectionService.evaluate(packet)) {
                    System.out.printf("[%s] %s from %s: %s (score=%.2f)%n",
                            alert.timestamp(), alert.type(), alert.sourceIp(), alert.detail(), alert.score());
                }
            }
        }
    }

    private static List<Packet> buildScenario(SyntheticTrafficGenerator generator, Instant start) {
        List<Packet> packets = new ArrayList<>();
        packets.addAll(generator.normalBaseline(start, 40, 5));
        packets.addAll(generator.lowRateSynTraffic(start, "203.0.113.9", 40, 1));
        packets.addAll(generator.synFloodBurst(start.plusSeconds(40), "203.0.113.9", 10, 200));
        packets.addAll(generator.portScanBurst(start.plusSeconds(55), "203.0.113.10", 30));
        packets.addAll(generator.dnsTunnelingQueries(start.plusSeconds(60), "10.0.0.15", 5));
        packets.sort(Comparator.comparing(Packet::timestamp));
        return packets;
    }
}
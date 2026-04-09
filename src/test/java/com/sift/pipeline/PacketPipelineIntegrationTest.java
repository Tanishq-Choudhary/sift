package com.sift.pipeline;

import com.sift.alerting.AlertPublisher;
import com.sift.capture.PcapFileReader;
import com.sift.capture.RawPacket;
import com.sift.detection.AlertJsonLineLogger;
import com.sift.detection.AlertRingBuffer;
import com.sift.detection.AnomalyDetectionService;
import com.sift.detection.DnsTunnelingDetector;
import com.sift.detection.PortScanDetector;
import com.sift.detection.SynFloodDetector;
import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.parsing.PacketParser;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PacketPipelineIntegrationTest {

    @Test
    void processesAllPacketsInPcapWithConsistentCounts() throws Exception {
        Path pcap = Path.of("src/main/resources/pcaps/mixed_traffic.pcap");
        List<RawPacket> rawPackets = new PcapFileReader().readAll(pcap);

        FlowTable flowTable = new FlowTable(1000);
        FlowTrackingService flowTrackingService = new FlowTrackingService(flowTable);
        AnomalyDetectionService detectionService = new AnomalyDetectionService(
                new SynFloodDetector(flowTrackingService),
                new PortScanDetector(),
                new DnsTunnelingDetector(),
                new AlertRingBuffer(500),
                mock(AlertPublisher.class),
                mock(AlertJsonLineLogger.class));

        PacketPipeline pipeline = new PacketPipeline(
        100,
        4,
        new PacketParser(),
        flowTrackingService,
        detectionService,
        new SimpleMeterRegistry());
        pipeline.start();

        int accepted = 0;
        for (RawPacket rawPacket : rawPackets) {
            if (pipeline.submit(rawPacket)) {
                accepted++;
            }
        }

        awaitDrain(pipeline, accepted);
        pipeline.shutdown();

        assertEquals(rawPackets.size(), pipeline.getPacketsReceived());
        assertEquals(0, pipeline.getPacketsDropped(), "queue capacity comfortably exceeds this pcap's size");
        assertEquals(accepted, pipeline.getPacketsProcessed() + pipeline.getPacketsUnparseable());
    }

    private void awaitDrain(PacketPipeline pipeline, int expectedAccepted) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            long done = pipeline.getPacketsProcessed() + pipeline.getPacketsUnparseable();
            if (done >= expectedAccepted) {
                return;
            }
            Thread.sleep(50);
        }
    }
}
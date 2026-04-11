package com.sift.pipeline;

import com.sift.alerting.AlertPublisher;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PacketPipelineBackpressureTest {

    @Test
    void dropsPacketsWhenQueueNeverDrains() throws Exception {
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
        1,
        1,
        new PacketParser(),
        flowTrackingService,
        detectionService,
        new SimpleMeterRegistry());
    

        byte[] garbageBytes = new byte[]{0x00};
        int totalSubmitted = 5;
        int accepted = 0;
        for (int i = 0; i < totalSubmitted; i++) {
            if (pipeline.submit(new RawPacket(garbageBytes, Instant.now()))) {
                accepted++;
            }
        }

        pipeline.shutdown();

        assertEquals(1, accepted, "only the first packet fits in a 1-slot queue that's never drained");
        assertEquals(totalSubmitted - accepted, pipeline.getPacketsDropped());
        assertEquals(totalSubmitted, pipeline.getPacketsReceived());
    }
}
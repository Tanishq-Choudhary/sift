package com.sift.api;

import com.sift.flow.FlowTable;
import com.sift.pipeline.PacketPipeline;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final PacketPipeline pipeline;
    private final FlowTable flowTable;

    public HealthController(PacketPipeline pipeline, FlowTable flowTable) {
        this.pipeline = pipeline;
        this.flowTable = flowTable;
    }

    @GetMapping("/api/health")
    public HealthStatus getHealth() {
        return new HealthStatus("UP", pipeline.getPacketsProcessed(), flowTable.size(), pipeline.getAlertsRaised(),
                pipeline.getPacketsDropped(), pipeline.getQueueDepth());
    }

    public record HealthStatus(String status, long packetsProcessed, int activeFlows, long alertsRaised,
                                long packetsDropped, int queueDepth) {}
}
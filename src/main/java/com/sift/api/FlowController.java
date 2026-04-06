package com.sift.api;

import com.sift.flow.FlowStats;
import com.sift.flow.FlowTable;
import com.sift.model.FlowKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
public class FlowController {

    private final FlowTable flowTable;

    public FlowController(FlowTable flowTable) {
        this.flowTable = flowTable;
    }

    @GetMapping("/api/flows/top")
    public FlowsResponse getTopFlows(
            @RequestParam(name = "by", defaultValue = "packetRate") String by,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {

        Instant now = Instant.now();
        Map<FlowKey, FlowStats> snapshot = flowTable.snapshot();

        Comparator<FlowSummary> comparator = "byteRate".equals(by)
                ? Comparator.comparingDouble(FlowSummary::byteRate)
                : Comparator.comparingDouble(FlowSummary::packetRate);

        List<FlowSummary> flows = snapshot.entrySet().stream()
                .map(entry -> toSummary(entry.getKey(), entry.getValue(), now))
                .sorted(comparator.reversed())
                .limit(limit)
                .toList();

        return new FlowsResponse(flows);
    }

    private FlowSummary toSummary(FlowKey key, FlowStats stats, Instant asOf) {
        return new FlowSummary(formatFlowKey(key), stats.getPacketRatePerSecond(asOf), byteRate(stats, asOf));
    }

    private String formatFlowKey(FlowKey key) {
        return "%s:%d -> %s:%d (%s)".formatted(key.srcIp(), key.srcPort(), key.dstIp(), key.dstPort(), key.protocol());
    }

    private double byteRate(FlowStats stats, Instant asOf) {
        long packetCount = stats.getPacketCount();
        if (packetCount == 0) {
            return 0.0;
        }
        double avgBytesPerPacket = (double) stats.getByteCount() / packetCount;
        return avgBytesPerPacket * stats.getPacketRatePerSecond(asOf);
    }

    public record FlowSummary(String flowKey, double packetRate, double byteRate) {}
    public record FlowsResponse(List<FlowSummary> flows) {}
}
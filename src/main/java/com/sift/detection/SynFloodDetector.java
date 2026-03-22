package com.sift.detection;

import com.sift.flow.FlowTrackingService;
import com.sift.model.Alert;
import com.sift.model.AlertType;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.stats.RunningStats;
import com.sift.stats.SlidingWindowCounter;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SynFloodDetector {

    private static final int RATE_WINDOW_SECONDS = 10;
    private static final int WARMUP_SECONDS = 30;
    private static final double DEFAULT_Z_SCORE_THRESHOLD = 3.0;

    private final FlowTrackingService flowTrackingService;
    private final double zScoreThreshold;
    private final Map<String, SlidingWindowCounter> synRateWindowsBySourceIp = new HashMap<>();
    private final Map<String, Instant> firstSeenBySourceIp = new HashMap<>();
    private final Set<String> sourcesCurrentlyFlooding = new HashSet<>();

    public SynFloodDetector(FlowTrackingService flowTrackingService) {
        this(flowTrackingService, DEFAULT_Z_SCORE_THRESHOLD);
    }

    public SynFloodDetector(FlowTrackingService flowTrackingService, double zScoreThreshold) {
        this.flowTrackingService = flowTrackingService;
        this.zScoreThreshold = zScoreThreshold;
    }

    public Optional<Alert> check(Packet packet) {
        if (packet.protocol() != Protocol.TCP || packet.tcpFlags() == null || !packet.tcpFlags().syn()) {
            return Optional.empty();
        }

        String sourceIp = packet.srcIp();
        Instant timestamp = packet.timestamp();
        firstSeenBySourceIp.putIfAbsent(sourceIp, timestamp);

        SlidingWindowCounter rateWindow = synRateWindowsBySourceIp.computeIfAbsent(
                sourceIp, key -> new SlidingWindowCounter(RATE_WINDOW_SECONDS));
        rateWindow.record(timestamp);

        Instant firstSeen = firstSeenBySourceIp.get(sourceIp);
        if (timestamp.getEpochSecond() - firstSeen.getEpochSecond() < WARMUP_SECONDS) {
            return Optional.empty();
        }

        RunningStats baseline = flowTrackingService.getSynRateStats(sourceIp);
        if (baseline == null || baseline.getCount() < 2) {
            return Optional.empty();
        }

        double currentRate = rateWindow.ratePerSecond(timestamp);
        double threshold = baseline.getMean() + zScoreThreshold * baseline.getStdDev();

        if (currentRate <= threshold) {
            sourcesCurrentlyFlooding.remove(sourceIp);
            return Optional.empty();
        }

        if (!sourcesCurrentlyFlooding.add(sourceIp)) {
            return Optional.empty();
        }

        double zScore = baseline.getStdDev() == 0.0
                ? Double.POSITIVE_INFINITY
                : (currentRate - baseline.getMean()) / baseline.getStdDev();

        return Optional.of(new Alert(
                AlertType.SYN_FLOOD,
                sourceIp,
                "SYN rate %.1f/s exceeds baseline mean %.1f/s + %.1f stddev (%.1f/s)".formatted(
                        currentRate, baseline.getMean(), zScoreThreshold, baseline.getStdDev()),
                zScore,
                timestamp));
    }
}
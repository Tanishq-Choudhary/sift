package com.sift.flow;

import com.sift.model.FlowKey;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.stats.RunningStats;
import com.sift.stats.SlidingWindowCounter;

import java.util.HashMap;
import java.util.Map;

public final class FlowTrackingService {

    private static final int SYN_RATE_WINDOW_SECONDS = 10;

    private final FlowTable flowTable;
    private final Map<String, SourceSynTracker> synTrackingBySourceIp = new HashMap<>();

    public FlowTrackingService(FlowTable flowTable) {
        this.flowTable = flowTable;
    }

    public FlowStats track(Packet packet) {
        FlowKey key = new FlowKey(
                packet.srcIp(), packet.dstIp(), packet.srcPort(), packet.dstPort(), packet.protocol());
        FlowStats stats = flowTable.getOrCreate(key);
        stats.recordPacket(packet);

        if (packet.protocol() == Protocol.TCP && packet.tcpFlags().syn()) {
            trackGlobalSynRate(packet);
        }

        return stats;
    }

    public RunningStats getSynRateStats(String sourceIp) {
        SourceSynTracker tracker = synTrackingBySourceIp.get(sourceIp);
        return tracker == null ? null : tracker.runningStats;
    }

    private void trackGlobalSynRate(Packet packet) {
        SourceSynTracker tracker =
                synTrackingBySourceIp.computeIfAbsent(packet.srcIp(), ip -> new SourceSynTracker());
        tracker.window.record(packet.timestamp());
        tracker.runningStats.update(tracker.window.ratePerSecond(packet.timestamp()));
    }

    private static final class SourceSynTracker {
        final SlidingWindowCounter window = new SlidingWindowCounter(SYN_RATE_WINDOW_SECONDS);
        final RunningStats runningStats = new RunningStats();
    }
}
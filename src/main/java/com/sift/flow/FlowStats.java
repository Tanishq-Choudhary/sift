package com.sift.flow;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.stats.RunningStats;
import com.sift.stats.SlidingWindowCounter;

import java.time.Instant;

public final class FlowStats {

    private static final int PACKET_RATE_WINDOW_SECONDS = 10;

    private long packetCount;
    private long byteCount;
    private long synCount;
    private Instant lastPacketTimestamp;
    private final RunningStats interArrivalTimeMillis = new RunningStats();
    private final SlidingWindowCounter packetRateWindow =
            new SlidingWindowCounter(PACKET_RATE_WINDOW_SECONDS);

    public void recordPacket(Packet packet) {
        packetCount++;
        byteCount += packet.payloadLen();
        packetRateWindow.record(packet.timestamp());

        if (lastPacketTimestamp != null) {
            long gapMillis = packet.timestamp().toEpochMilli() - lastPacketTimestamp.toEpochMilli();
            interArrivalTimeMillis.update(gapMillis);
        }
        lastPacketTimestamp = packet.timestamp();

        if (packet.protocol() == Protocol.TCP && packet.tcpFlags().syn()) {
            synCount++;
        }
    }

    public long getPacketCount() {
        return packetCount;
    }

    public long getByteCount() {
        return byteCount;
    }

    public long getSynCount() {
        return synCount;
    }

    public double getPacketRatePerSecond(Instant asOf) {
        return packetRateWindow.ratePerSecond(asOf);
    }

    public RunningStats getInterArrivalTimeMillis() {
        return interArrivalTimeMillis;
    }
}
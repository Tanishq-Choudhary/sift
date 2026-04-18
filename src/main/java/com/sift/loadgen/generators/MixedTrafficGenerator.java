package com.sift.loadgen.generators;

import com.sift.loadgen.LoadGeneratorProperties;
import com.sift.loadgen.LoadProfile;
import com.sift.model.Packet;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class MixedTrafficGenerator implements TrafficGenerator {

    private final TrafficGenerator normalGen;
    private final TrafficGenerator synFloodGen;
    private final TrafficGenerator portScanGen;
    private final TrafficGenerator dnsTunnelGen;

    private final int normalThreshold;
    private final int synFloodThreshold;
    private final int portScanThreshold;

    public MixedTrafficGenerator(
            TrafficGenerator normalGen,
            TrafficGenerator synFloodGen,
            TrafficGenerator portScanGen,
            TrafficGenerator dnsTunnelGen,
            LoadGeneratorProperties properties) {
        this.normalGen = normalGen;
        this.synFloodGen = synFloodGen;
        this.portScanGen = portScanGen;
        this.dnsTunnelGen = dnsTunnelGen;

        this.normalThreshold = properties.getNormalPercent();
        this.synFloodThreshold = this.normalThreshold + properties.getSynFloodPercent();
        this.portScanThreshold = this.synFloodThreshold + properties.getPortScanPercent();
    }

    @Override
    public Packet generatePacket(Instant now) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < normalThreshold) {
            return normalGen.generatePacket(now);
        } else if (roll < synFloodThreshold) {
            return synFloodGen.generatePacket(now);
        } else if (roll < portScanThreshold) {
            return portScanGen.generatePacket(now);
        } else {
            return dnsTunnelGen.generatePacket(now);
        }
    }

    @Override
    public LoadProfile getProfile() {
        return LoadProfile.MIXED;
    }
}
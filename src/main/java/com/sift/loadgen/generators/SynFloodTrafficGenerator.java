package com.sift.loadgen.generators;

import com.sift.loadgen.LoadProfile;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class SynFloodTrafficGenerator implements TrafficGenerator {

    private static final String ATTACKER_IP = "10.0.0.50";
    private static final String VICTIM_IP = "203.0.113.50";
    private static final TcpFlags SYN_FLAGS = new TcpFlags(true, false, false, false);

    @Override
    public Packet generatePacket(Instant now) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int srcPort = 1024 + random.nextInt(60_000);
        int dstPort = 80;

        return new Packet(ATTACKER_IP, VICTIM_IP, srcPort, dstPort, Protocol.TCP, SYN_FLAGS, 0, now, null);
    }

    @Override
    public LoadProfile getProfile() {
        return LoadProfile.SYN_FLOOD;
    }
}
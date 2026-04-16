package com.sift.loadgen.generators;

import com.sift.loadgen.LoadProfile;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class NormalTrafficGenerator implements TrafficGenerator {

    private static final String[] CLIENT_IPS = new String[100];
    private static final String SERVER_IP = "203.0.113.10";
    private static final int[] TARGET_PORTS = {80, 443, 8080};
    private static final TcpFlags ACK_FLAGS = new TcpFlags(false, true, false, false);
    private static final TcpFlags SYN_FLAGS = new TcpFlags(true, false, false, false);
    private static final String[] NORMAL_DOMAINS = {
            "www.example.com", "api.github.com", "cdn.cloudflare.com", "google.com", "aws.amazon.com"
    };

    static {
        for (int i = 0; i < 100; i++) {
            CLIENT_IPS[i] = "192.168.1." + (i + 1);
        }
    }

    @Override
    public Packet generatePacket(Instant now) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String srcIp = CLIENT_IPS[random.nextInt(CLIENT_IPS.length)];
        int srcPort = 1024 + random.nextInt(60_000);

        if (random.nextInt(100) < 90) {
            int dstPort = TARGET_PORTS[random.nextInt(TARGET_PORTS.length)];
            TcpFlags flags = random.nextInt(100) < 5 ? SYN_FLAGS : ACK_FLAGS;
            int payloadLen = 64 + random.nextInt(1336);
            return new Packet(srcIp, SERVER_IP, srcPort, dstPort, Protocol.TCP, flags, payloadLen, now, null);
        } else {
            String domain = NORMAL_DOMAINS[random.nextInt(NORMAL_DOMAINS.length)];
            return new Packet(srcIp, SERVER_IP, srcPort, 53, Protocol.UDP, null, 64, now, domain);
        }
    }

    @Override
    public LoadProfile getProfile() {
        return LoadProfile.NORMAL;
    }
}
package com.sift.loadgen.generators;

import com.sift.loadgen.LoadProfile;
import com.sift.model.Packet;
import com.sift.model.Protocol;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DnsTunnelTrafficGenerator implements TrafficGenerator {

    private static final String ATTACKER_IP = "10.0.0.52";
    private static final String VICTIM_IP = "203.0.113.50";
    private static final String[] TUNNEL_DOMAINS = new String[256];

    static {
        // Pre-allocate high-entropy query domains with subdomains >= 20 characters
        char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        Random rnd = new Random(42);
        for (int i = 0; i < TUNNEL_DOMAINS.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 24; j++) {
                sb.append(chars[rnd.nextInt(chars.length)]);
            }
            sb.append(".example.com");
            TUNNEL_DOMAINS[i] = sb.toString();
        }
    }

    @Override
    public Packet generatePacket(Instant now) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String domain = TUNNEL_DOMAINS[random.nextInt(TUNNEL_DOMAINS.length)];
        int srcPort = 1024 + random.nextInt(60_000);

        return new Packet(ATTACKER_IP, VICTIM_IP, srcPort, 53, Protocol.UDP, null, 128, now, domain);
    }

    @Override
    public LoadProfile getProfile() {
        return LoadProfile.DNS_TUNNEL;
    }
}
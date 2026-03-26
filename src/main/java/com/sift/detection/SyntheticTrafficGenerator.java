package com.sift.detection;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.model.TcpFlags;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SyntheticTrafficGenerator {

    private static final TcpFlags SYN_FLAGS = new TcpFlags(true, false, false, false);
    private static final TcpFlags ACK_FLAGS = new TcpFlags(false, true, false, false);
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final Random random = new Random(42);

    public List<Packet> normalBaseline(Instant start, int durationSeconds, int packetsPerSecond) {
        List<Packet> packets = new ArrayList<>();
        for (int second = 0; second < durationSeconds; second++) {
            Instant secondStart = start.plusSeconds(second);
            for (int i = 0; i < packetsPerSecond; i++) {
                String sourceIp = "10.0.0." + (10 + random.nextInt(20));
                int destPort = 80 + random.nextInt(3);
                packets.add(new Packet(sourceIp, "93.184.216.34", 40000 + random.nextInt(1000), destPort,
                        Protocol.TCP, ACK_FLAGS, 512, secondStart.plusMillis(random.nextInt(1000)), null));
            }
        }
        return packets;
    }

    public List<Packet> lowRateSynTraffic(Instant start, String sourceIp, int durationSeconds, int synsPerSecond) {
        List<Packet> packets = new ArrayList<>();
        for (int second = 0; second < durationSeconds; second++) {
            Instant secondStart = start.plusSeconds(second);
            for (int i = 0; i < synsPerSecond; i++) {
                packets.add(new Packet(sourceIp, "93.184.216.34", 49000 + i, 80,
                        Protocol.TCP, SYN_FLAGS, 0, secondStart.plusMillis(random.nextInt(1000)), null));
            }
        }
        return packets;
    }

    public List<Packet> synFloodBurst(Instant start, String attackerIp, int durationSeconds, int synsPerSecond) {
        List<Packet> packets = new ArrayList<>();
        for (int second = 0; second < durationSeconds; second++) {
            Instant secondStart = start.plusSeconds(second);
            for (int i = 0; i < synsPerSecond; i++) {
                packets.add(new Packet(attackerIp, "93.184.216.34", 50000 + i, 80,
                        Protocol.TCP, SYN_FLAGS, 0, secondStart.plusNanos(i * 1000L), null));
            }
        }
        return packets;
    }

    public List<Packet> portScanBurst(Instant start, String attackerIp, int uniquePorts) {
        List<Packet> packets = new ArrayList<>();
        for (int port = 1; port <= uniquePorts; port++) {
            packets.add(new Packet(attackerIp, "10.0.0.5", 51000, port,
                    Protocol.TCP, SYN_FLAGS, 0, start.plusMillis(port * 10L), null));
        }
        return packets;
    }

    public List<Packet> dnsTunnelingQueries(Instant start, String sourceIp, int count) {
        List<Packet> packets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String label = randomHighEntropyLabel(32);
            packets.add(new Packet(sourceIp, "8.8.8.8", 40000 + i, 53,
                    Protocol.UDP, null, 64, start.plusSeconds(i), label + ".tunnel.example.com"));
        }
        return packets;
    }

    private String randomHighEntropyLabel(int length) {
        StringBuilder label = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            label.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return label.toString();
    }
}
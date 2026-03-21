package com.sift.detection;

import com.sift.model.Alert;
import com.sift.model.AlertType;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.stats.SlidingWindowUniqueCounter;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PortScanDetector {

    private static final int DEFAULT_WINDOW_SECONDS = 10;
    private static final int DEFAULT_UNIQUE_PORT_THRESHOLD = 20;

    private final int windowSeconds;
    private final int uniquePortThreshold;
    private final Map<String, SlidingWindowUniqueCounter<Integer>> destPortWindowsBySourceIp = new HashMap<>();
    private final Set<String> sourcesCurrentlyScanning = new HashSet<>();

    public PortScanDetector() {
        this(DEFAULT_WINDOW_SECONDS, DEFAULT_UNIQUE_PORT_THRESHOLD);
    }

    public PortScanDetector(int windowSeconds, int uniquePortThreshold) {
        this.windowSeconds = windowSeconds;
        this.uniquePortThreshold = uniquePortThreshold;
    }

    public Optional<Alert> check(Packet packet) {
        if (packet.protocol() != Protocol.TCP && packet.protocol() != Protocol.UDP) {
            return Optional.empty();
        }

        String sourceIp = packet.srcIp();
        Instant timestamp = packet.timestamp();

        SlidingWindowUniqueCounter<Integer> destPortWindow = destPortWindowsBySourceIp.computeIfAbsent(
                sourceIp, key -> new SlidingWindowUniqueCounter<>(windowSeconds));
        destPortWindow.record(timestamp, packet.dstPort());

        int uniquePortCount = destPortWindow.uniqueCount(timestamp);

        if (uniquePortCount <= uniquePortThreshold) {
            sourcesCurrentlyScanning.remove(sourceIp);
            return Optional.empty();
        }

        if (!sourcesCurrentlyScanning.add(sourceIp)) {
            return Optional.empty();
        }

        return Optional.of(new Alert(
                AlertType.PORT_SCAN,
                sourceIp,
                "%d distinct destination ports contacted within %ds".formatted(uniquePortCount, windowSeconds),
                uniquePortCount,
                timestamp));
    }
}
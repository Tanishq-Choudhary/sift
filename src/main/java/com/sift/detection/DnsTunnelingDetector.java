package com.sift.detection;

import com.sift.model.Alert;
import com.sift.model.AlertType;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import com.sift.stats.ShannonEntropy;

import java.util.Optional;

public class DnsTunnelingDetector {

    private static final double ENTROPY_THRESHOLD_BITS_PER_CHAR = 3.5;
    private static final int LONG_LABEL_THRESHOLD_CHARS = 20;

    public Optional<Alert> check(Packet packet) {
        if (packet.protocol() != Protocol.UDP || packet.dnsQueryName() == null) {
            return Optional.empty();
        }

        String queryName = packet.dnsQueryName();
        double entropy = ShannonEntropy.bitsPerCharacter(queryName);
        int longestLabelLength = longestLabelLength(queryName);

        if (entropy <= ENTROPY_THRESHOLD_BITS_PER_CHAR || longestLabelLength < LONG_LABEL_THRESHOLD_CHARS) {
            return Optional.empty();
        }

        return Optional.of(new Alert(
                AlertType.DNS_TUNNELING,
                packet.srcIp(),
                "DNS query '%s' has entropy %.2f bits/char with a %d-char subdomain label".formatted(
                        queryName, entropy, longestLabelLength),
                entropy,
                packet.timestamp()));
    }

    private int longestLabelLength(String queryName) {
        int longest = 0;
        for (String label : queryName.split("\\.")) {
            longest = Math.max(longest, label.length());
        }
        return longest;
    }
}
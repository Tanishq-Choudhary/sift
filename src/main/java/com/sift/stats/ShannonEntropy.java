package com.sift.stats;

import java.util.HashMap;
import java.util.Map;

public final class ShannonEntropy {

    private ShannonEntropy() {
    }

    public static double bitsPerCharacter(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        Map<Character, Integer> frequencies = new HashMap<>();
        for (char c : text.toCharArray()) {
            frequencies.merge(c, 1, Integer::sum);
        }

        int length = text.length();
        double entropy = 0.0;
        for (int count : frequencies.values()) {
            double probability = (double) count / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }
}
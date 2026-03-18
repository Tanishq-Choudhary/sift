package com.sift.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunningStatsTest {

    @Test
    void matchesManuallyComputedMeanAndStdDev() {
        double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
        RunningStats stats = new RunningStats();
        for (double value : values) {
            stats.update(value);
        }

        assertEquals(5.0, stats.getMean(), 1e-9);
        assertEquals(2.13809, stats.getStdDev(), 1e-4);
    }

    @Test
    void singleValueHasZeroVariance() {
        RunningStats stats = new RunningStats();
        stats.update(42.0);

        assertEquals(42.0, stats.getMean(), 1e-9);
        assertEquals(0.0, stats.getVariance(), 1e-9);
    }
}
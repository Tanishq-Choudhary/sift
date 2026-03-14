package com.sift.stats;

import java.time.Instant;

public final class SlidingWindowCounter {

    private final long[] bucketCounts;
    private final int windowSeconds;
    private long currentBucketEpochSecond = -1;

    public SlidingWindowCounter(int windowSeconds) {
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        this.windowSeconds = windowSeconds;
        this.bucketCounts = new long[windowSeconds];
    }

    public void record(Instant timestamp) {
        recordN(timestamp, 1);
    }

    public void recordN(Instant timestamp, long amount) {
        long epochSecond = timestamp.getEpochSecond();
        advanceTo(epochSecond);
        bucketCounts[bucketIndex(epochSecond)] += amount;
    }

    public long sum(Instant asOf) {
        advanceTo(asOf.getEpochSecond());
        long total = 0;
        for (long bucketCount : bucketCounts) {
            total += bucketCount;
        }
        return total;
    }

    public double ratePerSecond(Instant asOf) {
        return sum(asOf) / (double) windowSeconds;
    }

    private void advanceTo(long epochSecond) {
        if (currentBucketEpochSecond < 0) {
            currentBucketEpochSecond = epochSecond;
            return;
        }
        long secondsElapsed = epochSecond - currentBucketEpochSecond;
        if (secondsElapsed <= 0) {
            return;
        }
       
        long bucketsToClear = Math.min(secondsElapsed, windowSeconds);
        for (long offset = 1; offset <= bucketsToClear; offset++) {
            bucketCounts[bucketIndex(currentBucketEpochSecond + offset)] = 0;
        }
        currentBucketEpochSecond = epochSecond;
    }

    private int bucketIndex(long epochSecond) {
        return (int) Math.floorMod(epochSecond, windowSeconds);
    }
}
package com.sift.stats;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SlidingWindowUniqueCounter<T> {

    private final int windowSeconds;
    private final Set<T>[] buckets;
    private final long[] bucketEpochSecond;

    @SuppressWarnings("unchecked")
    public SlidingWindowUniqueCounter(int windowSeconds) {
        this.windowSeconds = windowSeconds;
        this.buckets = new Set[windowSeconds];
        this.bucketEpochSecond = new long[windowSeconds];
        Arrays.fill(bucketEpochSecond, Long.MIN_VALUE);
        for (int i = 0; i < windowSeconds; i++) {
            buckets[i] = new HashSet<>();
        }
    }

    public void record(Instant timestamp, T item) {
        long second = timestamp.getEpochSecond();
        int index = bucketIndex(second);
        if (bucketEpochSecond[index] != second) {
            buckets[index].clear();
            bucketEpochSecond[index] = second;
        }
        buckets[index].add(item);
    }

    public int uniqueCount(Instant asOf) {
        long currentSecond = asOf.getEpochSecond();
        Set<T> union = new HashSet<>();
        for (int i = 0; i < windowSeconds; i++) {
            if (bucketEpochSecond[i] != Long.MIN_VALUE && currentSecond - bucketEpochSecond[i] < windowSeconds) {
                union.addAll(buckets[i]);
            }
        }
        return union.size();
    }

    private int bucketIndex(long epochSecond) {
        return (int) Math.floorMod(epochSecond, windowSeconds);
    }
}
package com.sift.stats;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SlidingWindowUniqueCounter<T> {

    private final int windowSeconds;
    private final Set<T>[] buckets;
    private final long[] bucketEpochSecond;
    private final Map<T, Integer> bucketsHoldingItem = new HashMap<>();

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
            clearBucket(index);
            bucketEpochSecond[index] = second;
        }
        if (buckets[index].add(item)) {
            bucketsHoldingItem.merge(item, 1, Integer::sum);
        }
    }

    public int uniqueCount(Instant asOf) {
        long currentSecond = asOf.getEpochSecond();
        for (int i = 0; i < windowSeconds; i++) {
            if (bucketEpochSecond[i] != Long.MIN_VALUE && currentSecond - bucketEpochSecond[i] >= windowSeconds) {
                clearBucket(i);
                bucketEpochSecond[i] = Long.MIN_VALUE;
            }
        }
        return bucketsHoldingItem.size();
    }

    private void clearBucket(int index) {
        for (T item : buckets[index]) {
            bucketsHoldingItem.computeIfPresent(item, (key, count) -> count == 1 ? null : count - 1);
        }
        buckets[index].clear();
    }

    private int bucketIndex(long epochSecond) {
        return (int) Math.floorMod(epochSecond, windowSeconds);
    }
}

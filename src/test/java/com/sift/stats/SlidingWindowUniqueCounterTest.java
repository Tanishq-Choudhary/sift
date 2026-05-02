package com.sift.stats;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlidingWindowUniqueCounterTest {

    @Test
    void countsDistinctItemsWithinWindow() {
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(10);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        for (int port = 1; port <= 15; port++) {
            counter.record(start.plusMillis(port * 100L), port);
        }

        assertEquals(15, counter.uniqueCount(start.plusSeconds(1)));
    }

    @Test
    void duplicateItemsWithinWindowCountOnce() {
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(10);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        counter.record(start, 80);
        counter.record(start.plusSeconds(1), 80);
        counter.record(start.plusSeconds(2), 443);

        assertEquals(2, counter.uniqueCount(start.plusSeconds(2)));
    }

    @Test
    void itemsOutsideWindowAreNotCounted() {
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(5);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        counter.record(start, 22);

        assertEquals(0, counter.uniqueCount(start.plusSeconds(20)));
    }

    @Test
    void itemStaysCountedWhileAnyBucketHoldingItIsLive() {
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(5);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        counter.record(start, 22);
        counter.record(start.plusSeconds(3), 22);

        assertEquals(1, counter.uniqueCount(start.plusSeconds(6)), "the second-3 copy is still in the window");
        assertEquals(0, counter.uniqueCount(start.plusSeconds(8)));
    }

    @Test
    void reusedBucketForgetsItsOldItems() {
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(5);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        counter.record(start, 22);
        counter.record(start.plusSeconds(5), 80);

        assertEquals(1, counter.uniqueCount(start.plusSeconds(5)));
    }

    @Test
    void matchesNaiveUnionOfBucketsOnRandomTraffic() {
        int window = 10;
        SlidingWindowUniqueCounter<Integer> counter = new SlidingWindowUniqueCounter<>(window);
        List<long[]> recorded = new ArrayList<>();
        Random random = new Random(7);
        long second = 1_767_225_600L;

        for (int i = 0; i < 20_000; i++) {
            second += random.nextInt(100) < 3 ? 1 + random.nextInt(4) : 0;
            int port = random.nextInt(random.nextBoolean() ? 30 : 2_000);
            counter.record(Instant.ofEpochSecond(second), port);
            recorded.add(new long[]{second, port});

            final long now = second;
            long expected = recorded.stream()
                    .filter(r -> now - r[0] < window)
                    .mapToLong(r -> r[1])
                    .distinct()
                    .count();
            if (i % 97 == 0) {
                assertEquals(expected, counter.uniqueCount(Instant.ofEpochSecond(now)), "at step " + i);
            }
            recorded.removeIf(r -> now - r[0] >= window);
        }
    }
}
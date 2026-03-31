package com.sift.stats;

import org.junit.jupiter.api.Test;

import java.time.Instant;

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
}
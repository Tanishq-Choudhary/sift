package com.sift.flow;

import com.sift.model.FlowKey;
import com.sift.model.Protocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlowTableTest {

    @Test
    void evictsLeastRecentlyUsedWhenOverCapacity() {
        FlowTable table = new FlowTable(3);
        FlowKey a = flowKey("10.0.0.1");
        FlowKey b = flowKey("10.0.0.2");
        FlowKey c = flowKey("10.0.0.3");
        FlowKey d = flowKey("10.0.0.4");
        FlowKey e = flowKey("10.0.0.5");

        table.getOrCreate(a);
        table.getOrCreate(b);
        table.getOrCreate(c);

        table.get(a);

        table.getOrCreate(d);
        table.getOrCreate(e);

        assertNotNull(table.get(a));
        assertNotNull(table.get(d));
        assertNotNull(table.get(e));
        assertNull(table.get(b));
        assertNull(table.get(c));
        assertEquals(3, table.size());
    }

    @Test
    void getOrCreateReturnsSameStatsInstanceForSameKey() {
        FlowTable table = new FlowTable(5);
        FlowKey key = flowKey("10.0.0.1");

        FlowStats first = table.getOrCreate(key);
        FlowStats second = table.getOrCreate(key);

        assertEquals(first, second);
    }

    private static FlowKey flowKey(String srcIp) {
        return new FlowKey(srcIp, "10.0.0.100", 1234, 80, Protocol.TCP);
    }
}
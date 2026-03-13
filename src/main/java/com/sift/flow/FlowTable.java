package com.sift.flow;

import com.sift.model.FlowKey;

import java.util.HashMap;
import java.util.Map;

public final class FlowTable {

    private final int capacity;
    private final Map<FlowKey, Node> nodesByKey;
    private Node head;
    private Node tail;

    public FlowTable(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.nodesByKey = new HashMap<>(capacity);
    }

    public FlowStats getOrCreate(FlowKey key) {
        Node existing = nodesByKey.get(key);
        if (existing != null) {
            moveToHead(existing);
            return existing.stats;
        }

        Node created = new Node(key, new FlowStats());
        nodesByKey.put(key, created);
        addToHead(created);

        if (nodesByKey.size() > capacity) {
            evictTail();
        }

        return created.stats;
    }

    public FlowStats get(FlowKey key) {
        Node node = nodesByKey.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.stats;
    }

    public int size() {
        return nodesByKey.size();
    }

    public Map<FlowKey, FlowStats> snapshot() {
        Map<FlowKey, FlowStats> copy = new HashMap<>(nodesByKey.size());
        for (Node node : nodesByKey.values()) {
            copy.put(node.key, node.stats);
        }
        return copy;
    }

    private void moveToHead(Node node) {
        if (node == head) {
            return;
        }
        unlink(node);
        addToHead(node);
    }

    private void addToHead(Node node) {
        node.prev = null;
        node.next = head;
        if (head != null) {
            head.prev = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
    }

    private void unlink(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        node.prev = null;
        node.next = null;
    }

    private void evictTail() {
        Node evicted = tail;
        unlink(evicted);
        nodesByKey.remove(evicted.key);
    }

    private static final class Node {
        final FlowKey key;
        final FlowStats stats;
        Node prev;
        Node next;

        Node(FlowKey key, FlowStats stats) {
            this.key = key;
            this.stats = stats;
        }
    }
}
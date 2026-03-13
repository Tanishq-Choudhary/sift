package com.sift.model;

public record FlowKey(String srcIp, String dstIp, int srcPort, int dstPort, Protocol protocol) {
}
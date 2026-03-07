package com.sift.model;

import java.time.Instant;

public record Packet(
        String srcIp,
        String dstIp,
        int srcPort,
        int dstPort,
        Protocol protocol,
        TcpFlags tcpFlags,
        int payloadLen,
        Instant timestamp,
        String dnsQueryName
) {
    public static final int NO_PORT = -1;
}
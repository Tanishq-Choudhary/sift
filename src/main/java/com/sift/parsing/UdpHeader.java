package com.sift.parsing;

public record UdpHeader(int sourcePort, int destinationPort, int length) {

    public static final int HEADER_LENGTH = 8;
}
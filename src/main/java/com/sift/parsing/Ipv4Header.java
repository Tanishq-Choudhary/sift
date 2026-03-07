package com.sift.parsing;

public record Ipv4Header(String sourceIp, String destinationIp, int protocolNumber,
                          int headerLength, int totalLength) {

    public static final int MIN_HEADER_LENGTH = 20;
}
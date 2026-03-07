package com.sift.parsing;

public record EthernetHeader(String destinationMac, String sourceMac, int etherType) {

    public static final int ETHER_TYPE_IPV4 = 0x0800;
    public static final int HEADER_LENGTH = 14;
}
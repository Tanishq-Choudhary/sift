package com.sift.model;

public enum Protocol {
    TCP,
    UDP,
    OTHER;

    public static Protocol fromIpProtocolNumber(int ipProtocolNumber) {
        return switch (ipProtocolNumber) {
            case 6 -> TCP;
            case 17 -> UDP;
            default -> OTHER;
        };
    }
}
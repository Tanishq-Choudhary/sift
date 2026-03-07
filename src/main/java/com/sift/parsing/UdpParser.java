package com.sift.parsing;

public final class UdpParser {

    private UdpParser() {
    }

    public static UdpHeader parse(byte[] data, int offset) {
        if (data.length < offset + UdpHeader.HEADER_LENGTH) {
            throw new PacketParseException("truncated udp header at offset " + offset);
        }

        int sourcePort = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        int destinationPort = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        int length = ((data[offset + 4] & 0xFF) << 8) | (data[offset + 5] & 0xFF);

        if (length < UdpHeader.HEADER_LENGTH || data.length < offset + length) {
            throw new PacketParseException("udp length " + length + " inconsistent with captured bytes");
        }

        return new UdpHeader(sourcePort, destinationPort, length);
    }
}
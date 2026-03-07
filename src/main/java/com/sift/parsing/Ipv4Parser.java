package com.sift.parsing;

public final class Ipv4Parser {

    private Ipv4Parser() {
    }

    public static Ipv4Header parse(byte[] data, int offset) {
        if (data.length < offset + Ipv4Header.MIN_HEADER_LENGTH) {
            throw new PacketParseException("truncated ipv4 header at offset " + offset);
        }

        int versionAndIhl = data[offset] & 0xFF;
        int version = versionAndIhl >>> 4;
        if (version != 4) {
            throw new PacketParseException("not an ipv4 packet, version=" + version);
        }
        int ihl = versionAndIhl & 0x0F;
        int headerLength = ihl * 4;
        if (headerLength < Ipv4Header.MIN_HEADER_LENGTH || data.length < offset + headerLength) {
            throw new PacketParseException("invalid ipv4 IHL=" + ihl);
        }

        int totalLength = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        if (totalLength < headerLength || data.length < offset + totalLength) {
            throw new PacketParseException("ipv4 totalLength " + totalLength + " inconsistent with captured bytes");
        }

        int protocolNumber = data[offset + 9] & 0xFF;
        String sourceIp = formatIp(data, offset + 12);
        String destinationIp = formatIp(data, offset + 16);

        return new Ipv4Header(sourceIp, destinationIp, protocolNumber, headerLength, totalLength);
    }

    private static String formatIp(byte[] data, int offset) {
        return (data[offset] & 0xFF) + "." + (data[offset + 1] & 0xFF) + "."
                + (data[offset + 2] & 0xFF) + "." + (data[offset + 3] & 0xFF);
    }
}
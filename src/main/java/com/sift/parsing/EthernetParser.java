package com.sift.parsing;

public final class EthernetParser {

    private EthernetParser() {
    }

    public static EthernetHeader parse(byte[] data) {
        if (data.length < EthernetHeader.HEADER_LENGTH) {
            throw new PacketParseException("truncated ethernet header: " + data.length + " bytes");
        }
        String destinationMac = formatMac(data, 0);
        String sourceMac = formatMac(data, 6);
        int etherType = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);
        return new EthernetHeader(destinationMac, sourceMac, etherType);
    }

    private static String formatMac(byte[] data, int offset) {
        StringBuilder mac = new StringBuilder(17);
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                mac.append(':');
            }
            mac.append(String.format("%02x", data[offset + i]));
        }
        return mac.toString();
    }
}
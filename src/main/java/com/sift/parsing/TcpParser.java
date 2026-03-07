package com.sift.parsing;

import com.sift.model.TcpFlags;

public final class TcpParser {

    private TcpParser() {
    }

    public static TcpHeader parse(byte[] data, int offset) {
        if (data.length < offset + TcpHeader.MIN_HEADER_LENGTH) {
            throw new PacketParseException("truncated tcp header at offset " + offset);
        }

        int sourcePort = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        int destinationPort = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        long sequenceNumber = ((long) (data[offset + 4] & 0xFF) << 24)
                | ((data[offset + 5] & 0xFF) << 16)
                | ((data[offset + 6] & 0xFF) << 8)
                | (data[offset + 7] & 0xFF);

        int dataOffsetWords = (data[offset + 12] & 0xFF) >>> 4;
        int headerLength = dataOffsetWords * 4;
        if (headerLength < TcpHeader.MIN_HEADER_LENGTH || data.length < offset + headerLength) {
            throw new PacketParseException("invalid tcp data offset=" + dataOffsetWords);
        }

        int flagsByte = data[offset + 13] & 0xFF;
        TcpFlags flags = new TcpFlags(
                (flagsByte & 0x02) != 0,
                (flagsByte & 0x10) != 0,
                (flagsByte & 0x01) != 0,
                (flagsByte & 0x04) != 0
        );

        return new TcpHeader(sourcePort, destinationPort, flags, sequenceNumber, headerLength);
    }
}
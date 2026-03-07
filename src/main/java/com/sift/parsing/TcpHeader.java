package com.sift.parsing;

import com.sift.model.TcpFlags;

public record TcpHeader(int sourcePort, int destinationPort, TcpFlags flags,
                         long sequenceNumber, int headerLength) {

    public static final int MIN_HEADER_LENGTH = 20;
}
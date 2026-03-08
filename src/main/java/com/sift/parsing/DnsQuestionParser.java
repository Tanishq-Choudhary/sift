package com.sift.parsing;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class DnsQuestionParser {

    private static final int DNS_HEADER_LENGTH = 12;
    private static final int MAX_NAME_LENGTH = 253;
    private static final int MAX_LABEL_LENGTH = 63;
    private static final int COMPRESSION_POINTER_MASK = 0xC0;

    private DnsQuestionParser() {
    }

    public static Optional<String> parseQueryName(byte[] data, int offset, int limit) {
        if (limit - offset < DNS_HEADER_LENGTH) {
            return Optional.empty();
        }

        int questionCount = ((data[offset + 4] & 0xFF) << 8) | (data[offset + 5] & 0xFF);
        if (questionCount < 1) {
            return Optional.empty();
        }

        StringBuilder queryName = new StringBuilder();
        int position = offset + DNS_HEADER_LENGTH;

        while (position < limit) {
            int labelLength = data[position] & 0xFF;
            if (labelLength == 0) {
                break;
            }
            if ((labelLength & COMPRESSION_POINTER_MASK) == COMPRESSION_POINTER_MASK) {
                return Optional.empty();
            }
            if (labelLength > MAX_LABEL_LENGTH) {
                return Optional.empty();
            }

            position++;
            if (position + labelLength > limit) {
                return Optional.empty();
            }

            if (queryName.length() > 0) {
                queryName.append('.');
            }
            queryName.append(new String(data, position, labelLength, StandardCharsets.US_ASCII));
            position += labelLength;

            if (queryName.length() > MAX_NAME_LENGTH) {
                return Optional.empty();
            }
        }

        return queryName.length() == 0 ? Optional.empty() : Optional.of(queryName.toString());
    }
}
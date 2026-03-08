package com.sift.capture;

import java.time.Instant;

public record RawPacket(byte[] data, Instant captureTime) {
}
package com.sift.model;

import java.time.Instant;

public record Alert(AlertType type, String sourceIp, String detail, double score, Instant timestamp) {
}
package com.sift.model;

public record TcpFlags(boolean syn, boolean ack, boolean fin, boolean rst) {
}
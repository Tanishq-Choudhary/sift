package com.sift.loadgen.generators;

import com.sift.loadgen.LoadProfile;
import com.sift.model.Packet;

import java.time.Instant;

public interface TrafficGenerator {
    Packet generatePacket(Instant now);
    LoadProfile getProfile();
}
package com.sift.capture;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class PcapFileReader {

    private static final Logger logger = LoggerFactory.getLogger(PcapFileReader.class);

    public List<RawPacket> readAll(Path pcapFile) throws PcapNativeException, NotOpenException {
        List<RawPacket> rawPackets = new ArrayList<>();
        PcapHandle handle = Pcaps.openOffline(pcapFile.toString());
        try {
            byte[] data;
            while ((data = handle.getNextRawPacket()) != null) {
                rawPackets.add(new RawPacket(data, resolveTimestamp(handle)));
            }
        } finally {
            handle.close();
        }
        return rawPackets;
    }

    private Instant resolveTimestamp(PcapHandle handle) {
        Timestamp timestamp = handle.getTimestamp();
        if (timestamp == null) {
            logger.warn("pcap4j returned a null capture timestamp, falling back to Instant.now()");
            return Instant.now();
        }
        return timestamp.toInstant();
    }
}
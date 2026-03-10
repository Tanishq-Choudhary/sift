package com.sift.capture;

import com.sift.model.Packet;
import com.sift.parsing.PacketParser;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class PcapConsoleDemo {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: PcapConsoleDemo <path-to-pcap-file>");
            System.exit(1);
        }

        PcapFileReader reader = new PcapFileReader();
        PacketParser parser = new PacketParser();

        List<RawPacket> rawPackets = reader.readAll(Path.of(args[0]));
        int parsedCount = 0;
        int skippedCount = 0;

        for (RawPacket rawPacket : rawPackets) {
            Optional<Packet> packet = parser.parse(rawPacket.data(), rawPacket.captureTime());
            if (packet.isPresent()) {
                System.out.println(packet.get());
                parsedCount++;
            } else {
                skippedCount++;
            }
        }

        System.out.printf("%n%d packets read, %d parsed, %d skipped%n",
                rawPackets.size(), parsedCount, skippedCount);
    }
}
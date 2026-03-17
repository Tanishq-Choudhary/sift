package com.sift.flow;

import com.sift.capture.PcapFileReader;
import com.sift.capture.RawPacket;
import com.sift.model.FlowKey;
import com.sift.model.Packet;
import com.sift.parsing.PacketParser;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FlowConsoleDemo {

    private static final int DEFAULT_FLOW_CAPACITY = 50_000;
    private static final int TOP_N = 10;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: FlowConsoleDemo <path-to-pcap>");
            System.exit(1);
        }

        Path pcapFile = Path.of(args[0]);
        PcapFileReader reader = new PcapFileReader();
        PacketParser parser = new PacketParser();
        FlowTable flowTable = new FlowTable(DEFAULT_FLOW_CAPACITY);
        FlowTrackingService trackingService = new FlowTrackingService(flowTable);

        List<RawPacket> rawPackets = reader.readAll(pcapFile);
        int parsedCount = 0;
        for (RawPacket rawPacket : rawPackets) {
            Optional<Packet> packet = parser.parse(rawPacket.data(), rawPacket.captureTime());
            if (packet.isPresent()) {
                trackingService.track(packet.get());
                parsedCount++;
            }
        }

        System.out.printf("%d packets read, %d parsed, %d flows tracked%n",
                rawPackets.size(), parsedCount, flowTable.size());

        Map<FlowKey, FlowStats> snapshot = flowTable.snapshot();
        snapshot.entrySet().stream()
                .sorted(Comparator.<Map.Entry<FlowKey, FlowStats>>comparingLong(
                        entry -> entry.getValue().getPacketCount()).reversed())
                .limit(TOP_N)
                .forEach(entry -> System.out.printf("%s -> %d packets, %d bytes%n",
                        entry.getKey(), entry.getValue().getPacketCount(),
                        entry.getValue().getByteCount()));
    }
}
package com.sift.pipeline;

import com.sift.capture.PcapFileReader;
import com.sift.capture.RawPacket;
import com.sift.config.SiftProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;


@Component
public class PcapReplayRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PcapReplayRunner.class);

    private final PcapFileReader pcapFileReader;
    private final PacketPipeline pipeline;
    private final Path pcapFile;

    public PcapReplayRunner(PcapFileReader pcapFileReader, PacketPipeline pipeline, SiftProperties properties) {
        this.pcapFileReader = pcapFileReader;
        this.pipeline = pipeline;
        this.pcapFile = properties.getPcapFile().isBlank() ? null : Path.of(properties.getPcapFile());
    }

    @Override
    public void run(String... args) throws Exception {
        pipeline.start();
        if (pcapFile == null) {
            log.info("No sift.pcap-file configured; pipeline started idle.");
            return;
        }
        List<RawPacket> rawPackets = pcapFileReader.readAll(pcapFile);
        log.info("Replaying {} packets from {}", rawPackets.size(), pcapFile);
        int dropped = 0;
        for (RawPacket rawPacket : rawPackets) {
            if (!pipeline.submit(rawPacket)) {
                dropped++;
            }
        }
        log.info("Replay submitted: {} packets, {} dropped at enqueue", rawPackets.size(), dropped);
    }
}
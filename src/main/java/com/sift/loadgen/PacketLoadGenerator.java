package com.sift.loadgen;

import com.sift.loadgen.generators.DnsTunnelTrafficGenerator;
import com.sift.loadgen.generators.MixedTrafficGenerator;
import com.sift.loadgen.generators.NormalTrafficGenerator;
import com.sift.loadgen.generators.PortScanTrafficGenerator;
import com.sift.loadgen.generators.SynFloodTrafficGenerator;
import com.sift.loadgen.generators.TrafficGenerator;
import com.sift.model.Packet;
import com.sift.pipeline.PacketPipeline;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@Component
@Profile("loadtest")
public class PacketLoadGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PacketLoadGenerator.class);

    private final PacketPipeline pipeline;
    private final LoadGeneratorProperties properties;
    private final MeterRegistry meterRegistry;
    private final TrafficGenerator trafficGenerator;

    private final AtomicLong submittedCount = new AtomicLong();
    private final AtomicLong rejectedCount = new AtomicLong();
    private volatile double currentThroughput = 0.0;

    public PacketLoadGenerator(PacketPipeline pipeline,
                               LoadGeneratorProperties properties,
                               MeterRegistry meterRegistry) {
        this.pipeline = pipeline;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.trafficGenerator = createTrafficGenerator(properties);

        registerMetrics();
    }

    private TrafficGenerator createTrafficGenerator(LoadGeneratorProperties props) {
        TrafficGenerator normal = new NormalTrafficGenerator();
        TrafficGenerator synFlood = new SynFloodTrafficGenerator();
        TrafficGenerator portScan = new PortScanTrafficGenerator();
        TrafficGenerator dnsTunnel = new DnsTunnelTrafficGenerator();

        return switch (props.getProfile()) {
            case NORMAL -> normal;
            case SYN_FLOOD -> synFlood;
            case PORT_SCAN -> portScan;
            case DNS_TUNNEL -> dnsTunnel;
            case MIXED -> new MixedTrafficGenerator(normal, synFlood, portScan, dnsTunnel, props);
        };
    }

    private void registerMetrics() {
        Tags tags = Tags.of("profile", properties.getProfile().name());
        Gauge.builder("sift.loadgen.target_rate", properties, LoadGeneratorProperties::getRatePerSecond)
                .tags(tags).register(meterRegistry);
        Gauge.builder("sift.loadgen.throughput", this, PacketLoadGenerator::getCurrentThroughput)
                .tags(tags).register(meterRegistry);
        Gauge.builder("sift.loadgen.submitted", submittedCount, AtomicLong::get)
                .tags(tags).register(meterRegistry);
        Gauge.builder("sift.loadgen.rejected", rejectedCount, AtomicLong::get)
                .tags(tags).register(meterRegistry);
    }

    @Override
    public void run(String... args) {
        pipeline.start();

        long totalPackets = properties.getTotalPackets();
        int targetRate = properties.getRatePerSecond();
        int workerThreads = Math.max(1, properties.getWorkerThreads());

        log.info("Starting Sift Load Generator. Profile: {}, Total: {}, Target Rate: {} pkt/s, Workers: {}",
                properties.getProfile(), totalPackets, targetRate, workerThreads);

        ExecutorService executor = Executors.newFixedThreadPool(workerThreads);
        List<Future<?>> futures = new ArrayList<>();

        long packetsPerWorker = totalPackets / workerThreads;
        long remainder = totalPackets % workerThreads;
        int workerTargetRate = Math.max(1, targetRate / workerThreads);

        Instant startTime = Instant.now();

        for (int i = 0; i < workerThreads; i++) {
            long countToSubmit = packetsPerWorker + (i == 0 ? remainder : 0);
            futures.add(executor.submit(() -> generateWorkerLoop(countToSubmit, workerTargetRate)));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error during load generation execution", e);
            }
        }

        executor.shutdown();
        drainPipelineQueue();

        Duration elapsed = Duration.between(startTime, Instant.now());
        long elapsedMs = Math.max(1, elapsed.toMillis());
        long totalSubmitted = submittedCount.get();
        this.currentThroughput = (totalSubmitted * 1000.0) / elapsedMs;

        logSummary(totalSubmitted, elapsedMs, currentThroughput);
    }

    private void generateWorkerLoop(long countToSubmit, int ratePerSecond) {
        long intervalNanos = ratePerSecond > 0 ? 1_000_000_000L / ratePerSecond : 0;
        long nextSendNanos = System.nanoTime();

        for (long i = 0; i < countToSubmit; i++) {
            Instant now = Instant.now();
            Packet packet = trafficGenerator.generatePacket(now);

            if (pipeline.submit(packet)) {
                submittedCount.incrementAndGet();
            } else {
                rejectedCount.incrementAndGet();
            }

            if (intervalNanos > 0) {
                nextSendNanos += intervalNanos;
                long waitNanos = nextSendNanos - System.nanoTime();
                if (waitNanos > 0) {
                    LockSupport.parkNanos(waitNanos);
                }
            }
        }
    }

    private void drainPipelineQueue() {
        int waited = 0;
        while (pipeline.getQueueDepth() > 0 && waited < 100) {
            try {
                Thread.sleep(100);
                waited++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void logSummary(long totalSubmitted, long elapsedMs, double throughput) {
        log.info("""
                
                ==================================================
                LOAD GENERATION SUMMARY
                ==================================================
                Load profile: {}
                Target rate:  {} pkt/s
                Submitted:    {}
                Processed:    {}
                Dropped:      {}
                Alerts:       {}
                Elapsed:      {} ms
                Throughput:   %.2f pkt/s
                ==================================================
                """,
                properties.getProfile(),
                properties.getRatePerSecond(),
                totalSubmitted,
                pipeline.getPacketsProcessed(),
                pipeline.getPacketsDropped() + rejectedCount.get(),
                pipeline.getAlertsRaised(),
                elapsedMs,
                throughput);
    }

    public long getSubmittedCount() { return submittedCount.get(); }
    public long getRejectedCount() { return rejectedCount.get(); }
    public double getCurrentThroughput() { return currentThroughput; }
}
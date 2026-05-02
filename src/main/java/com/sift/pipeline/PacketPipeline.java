package com.sift.pipeline;

import com.sift.capture.RawPacket;
import com.sift.detection.AnomalyDetectionService;
import com.sift.flow.FlowTrackingService;
import com.sift.model.Alert;
import com.sift.model.Packet;
import com.sift.parsing.PacketParser;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PacketPipeline {

    private static final long ENQUEUE_TIMEOUT_MILLIS = 200;

    private sealed interface QueueItem {
        record Raw(RawPacket rawPacket) implements QueueItem {}
        record Parsed(Packet packet) implements QueueItem {}
    }

    private final BlockingQueue<QueueItem> queue;
    private final ExecutorService workers;
    private final int workerCount;
    private final PacketParser packetParser;
    private final FlowTrackingService flowTrackingService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final Object detectionLock = new Object();
    private final MeterRegistry meterRegistry;
    private final Timer processingLatencyTimer;

    private final AtomicLong packetsReceived = new AtomicLong();
    private final AtomicLong packetsProcessed = new AtomicLong();
    private final AtomicLong packetsDropped = new AtomicLong();
    private final AtomicLong packetsUnparseable = new AtomicLong();
    private final AtomicLong alertsRaised = new AtomicLong();

    private volatile boolean running = false;

    public PacketPipeline(int queueCapacity, int workerCount, PacketParser packetParser,
                           FlowTrackingService flowTrackingService,
                           AnomalyDetectionService anomalyDetectionService,
                           MeterRegistry meterRegistry) {
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.workerCount = workerCount;
        this.workers = Executors.newFixedThreadPool(workerCount);
        this.packetParser = packetParser;
        this.flowTrackingService = flowTrackingService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.meterRegistry = meterRegistry;

        FunctionCounter.builder("sift.packets.processed", packetsProcessed, AtomicLong::get)
                .register(meterRegistry);
        FunctionCounter.builder("sift.packets.dropped", packetsDropped, AtomicLong::get)
                .register(meterRegistry);
        FunctionCounter.builder("sift.alerts.raised", alertsRaised, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("sift.queue.depth", queue, BlockingQueue::size)
                .register(meterRegistry);
        this.processingLatencyTimer = Timer.builder("sift.packet.processing.latency")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::runWorkerLoop);
        }
    }

    public boolean submit(RawPacket rawPacket) {
        return enqueue(new QueueItem.Raw(rawPacket));
    }

    public boolean submit(Packet packet) {
        return enqueue(new QueueItem.Parsed(packet));
    }

    private boolean enqueue(QueueItem item) {
        packetsReceived.incrementAndGet();
        try {
            boolean accepted = queue.offer(item, ENQUEUE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!accepted) {
                packetsDropped.incrementAndGet();
            }
            return accepted;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            packetsDropped.incrementAndGet();
            return false;
        }
    }

    private void runWorkerLoop() {
        while (running) {
            QueueItem item;
            try {
                item = queue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (item == null) {
                continue;
            }
            processQueueItem(item);
        }
    }

    private void processQueueItem(QueueItem item) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Optional<Packet> parsed = switch (item) {
                case QueueItem.Raw raw -> packetParser.parse(raw.rawPacket().data(), raw.rawPacket().captureTime());
                case QueueItem.Parsed parsedItem -> Optional.of(parsedItem.packet());
            };
            if (parsed.isEmpty()) {
                packetsUnparseable.incrementAndGet();
                return;
            }
            Packet packet = parsed.get();
            List<Alert> firedAlerts;
            synchronized (detectionLock) {
                flowTrackingService.track(packet);
                firedAlerts = anomalyDetectionService.detect(packet);
            }
            anomalyDetectionService.raise(firedAlerts);
            alertsRaised.addAndGet(firedAlerts.size());
            packetsProcessed.incrementAndGet();
        } finally {
            sample.stop(processingLatencyTimer);
        }
    }

    public void shutdown() {
        running = false;
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public long getPacketsReceived() { return packetsReceived.get(); }
    public long getPacketsProcessed() { return packetsProcessed.get(); }
    public long getPacketsDropped() { return packetsDropped.get(); }
    public long getPacketsUnparseable() { return packetsUnparseable.get(); }
    public long getAlertsRaised() { return alertsRaised.get(); }
    public int getQueueDepth() { return queue.size(); }
}
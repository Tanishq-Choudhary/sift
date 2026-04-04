package com.sift.config;

import com.sift.alerting.AlertPublisher;
import com.sift.capture.PcapFileReader;
import com.sift.detection.AlertJsonLineLogger;
import com.sift.detection.AlertRingBuffer;
import com.sift.detection.AnomalyDetectionService;
import com.sift.detection.DnsTunnelingDetector;
import com.sift.detection.PortScanDetector;
import com.sift.detection.SynFloodDetector;
import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.loadgen.LoadGeneratorProperties;
import com.sift.parsing.PacketParser;
import com.sift.pipeline.PacketPipeline;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties({SiftProperties.class, LoadGeneratorProperties.class})
public class PipelineConfig {

    @Bean
    public PacketParser packetParser() {
        return new PacketParser();
    }

    @Bean
    public PcapFileReader pcapFileReader() {
        return new PcapFileReader();
    }

    @Bean
    public FlowTable flowTable(SiftProperties properties, MeterRegistry meterRegistry) {
        FlowTable flowTable = new FlowTable(properties.getFlowTableCapacity());
        Gauge.builder("sift.flows.active", flowTable, FlowTable::size).register(meterRegistry);
        return flowTable;
    }

    @Bean
    public FlowTrackingService flowTrackingService(FlowTable flowTable) {
        return new FlowTrackingService(flowTable);
    }

    @Bean
    public SynFloodDetector synFloodDetector(FlowTrackingService flowTrackingService, SiftProperties properties) {
        return new SynFloodDetector(flowTrackingService, properties.getSynFloodZScoreThreshold());
    }

    @Bean
    public PortScanDetector portScanDetector(SiftProperties properties) {
        return new PortScanDetector(properties.getPortScanWindowSeconds(), properties.getPortScanUniquePortThreshold());
    }

    @Bean
    public DnsTunnelingDetector dnsTunnelingDetector() {
        return new DnsTunnelingDetector();
    }

    @Bean
    public AlertRingBuffer alertRingBuffer(SiftProperties properties) {
        return new AlertRingBuffer(properties.getAlertRingBufferCapacity());
    }

    @Bean(destroyMethod = "close")
    public AlertPublisher alertPublisher(SiftProperties properties) throws Exception {
        return new AlertPublisher(properties.getRabbitMqHost(), properties.getRabbitMqPort());
    }

    @Bean
    public AlertJsonLineLogger alertJsonLineLogger(SiftProperties properties) {
        return new AlertJsonLineLogger(Path.of(properties.getAlertLogFile()));
    }

    @Bean
    public AnomalyDetectionService anomalyDetectionService(SynFloodDetector synFloodDetector,
                                                             PortScanDetector portScanDetector,
                                                             DnsTunnelingDetector dnsTunnelingDetector,
                                                             AlertRingBuffer alertRingBuffer,
                                                             AlertPublisher alertPublisher,
                                                             AlertJsonLineLogger alertJsonLineLogger) {
        return new AnomalyDetectionService(synFloodDetector, portScanDetector, dnsTunnelingDetector,
                alertRingBuffer, alertPublisher, alertJsonLineLogger);
    }

    @Bean(destroyMethod = "shutdown")
    public PacketPipeline packetPipeline(PacketParser packetParser, FlowTrackingService flowTrackingService,
                                          AnomalyDetectionService anomalyDetectionService, SiftProperties properties,
                                          MeterRegistry meterRegistry) {
        return new PacketPipeline(properties.getPipelineQueueCapacity(), properties.getPipelineWorkerCount(),
                packetParser, flowTrackingService, anomalyDetectionService, meterRegistry);
    }
}
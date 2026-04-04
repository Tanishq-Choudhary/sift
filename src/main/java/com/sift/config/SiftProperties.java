package com.sift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sift")
public class SiftProperties {

    private int flowTableCapacity = 50_000;
    private int alertRingBufferCapacity = 500;
    private int pipelineQueueCapacity = 10_000;
    private int pipelineWorkerCount = 4;
    private double synFloodZScoreThreshold = 3.0;
    private int portScanWindowSeconds = 10;
    private int portScanUniquePortThreshold = 20;
    private String rabbitMqHost = "localhost";
    private int rabbitMqPort = 5672;
    private String alertLogFile = "sift-alerts.jsonl";
    private String pcapFile = "";

    public int getFlowTableCapacity() { return flowTableCapacity; }
    public void setFlowTableCapacity(int flowTableCapacity) { this.flowTableCapacity = flowTableCapacity; }

    public int getAlertRingBufferCapacity() { return alertRingBufferCapacity; }
    public void setAlertRingBufferCapacity(int alertRingBufferCapacity) { this.alertRingBufferCapacity = alertRingBufferCapacity; }

    public int getPipelineQueueCapacity() { return pipelineQueueCapacity; }
    public void setPipelineQueueCapacity(int pipelineQueueCapacity) { this.pipelineQueueCapacity = pipelineQueueCapacity; }

    public int getPipelineWorkerCount() { return pipelineWorkerCount; }
    public void setPipelineWorkerCount(int pipelineWorkerCount) { this.pipelineWorkerCount = pipelineWorkerCount; }

    public double getSynFloodZScoreThreshold() { return synFloodZScoreThreshold; }
    public void setSynFloodZScoreThreshold(double synFloodZScoreThreshold) { this.synFloodZScoreThreshold = synFloodZScoreThreshold; }

    public int getPortScanWindowSeconds() { return portScanWindowSeconds; }
    public void setPortScanWindowSeconds(int portScanWindowSeconds) { this.portScanWindowSeconds = portScanWindowSeconds; }

    public int getPortScanUniquePortThreshold() { return portScanUniquePortThreshold; }
    public void setPortScanUniquePortThreshold(int portScanUniquePortThreshold) { this.portScanUniquePortThreshold = portScanUniquePortThreshold; }

    public String getRabbitMqHost() { return rabbitMqHost; }
    public void setRabbitMqHost(String rabbitMqHost) { this.rabbitMqHost = rabbitMqHost; }

    public int getRabbitMqPort() { return rabbitMqPort; }
    public void setRabbitMqPort(int rabbitMqPort) { this.rabbitMqPort = rabbitMqPort; }

    public String getAlertLogFile() { return alertLogFile; }
    public void setAlertLogFile(String alertLogFile) { this.alertLogFile = alertLogFile; }

    public String getPcapFile() { return pcapFile; }
    public void setPcapFile(String pcapFile) { this.pcapFile = pcapFile; }
}
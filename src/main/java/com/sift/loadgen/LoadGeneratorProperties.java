package com.sift.loadgen;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sift.loadgen")
public class LoadGeneratorProperties {

    private LoadProfile profile = LoadProfile.MIXED;
    private long totalPackets = 1_000_000L;
    private int ratePerSecond = 50_000;
    private int normalPercent = 70;
    private int synFloodPercent = 20;
    private int portScanPercent = 7;
    private int dnsTunnelPercent = 3;
    private int workerThreads = Runtime.getRuntime().availableProcessors();

    @PostConstruct
    public void validate() {
        if (totalPackets <= 0) {
            throw new IllegalArgumentException("total-packets must be positive");
        }
        if (ratePerSecond <= 0) {
            throw new IllegalArgumentException("rate-per-second must be positive");
        }
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("worker-threads must be positive");
        }
        if (profile == LoadProfile.MIXED) {
            int sum = normalPercent + synFloodPercent + portScanPercent + dnsTunnelPercent;
            if (sum != 100) {
                throw new IllegalArgumentException(
                        "Percentages must sum to 100 for MIXED profile. Current sum: " + sum);
            }
        }
    }

    public LoadProfile getProfile() {
        return profile;
    }

    public void setProfile(LoadProfile profile) {
        this.profile = profile;
    }

    public long getTotalPackets() {
        return totalPackets;
    }

    public void setTotalPackets(long totalPackets) {
        this.totalPackets = totalPackets;
    }

    public int getRatePerSecond() {
        return ratePerSecond;
    }

    public void setRatePerSecond(int ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
    }

    public int getNormalPercent() {
        return normalPercent;
    }

    public void setNormalPercent(int normalPercent) {
        this.normalPercent = normalPercent;
    }

    public int getSynFloodPercent() {
        return synFloodPercent;
    }

    public void setSynFloodPercent(int synFloodPercent) {
        this.synFloodPercent = synFloodPercent;
    }

    public int getPortScanPercent() {
        return portScanPercent;
    }

    public void setPortScanPercent(int portScanPercent) {
        this.portScanPercent = portScanPercent;
    }

    public int getDnsTunnelPercent() {
        return dnsTunnelPercent;
    }

    public void setDnsTunnelPercent(int dnsTunnelPercent) {
        this.dnsTunnelPercent = dnsTunnelPercent;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
}
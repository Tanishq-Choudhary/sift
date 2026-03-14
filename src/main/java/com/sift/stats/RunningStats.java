package com.sift.stats;

public final class RunningStats {

    private long count;
    private double mean;
    private double sumSquaredDeltas;

    public void update(double value) {
        count++;
        double deltaBeforeMeanUpdate = value - mean;
        mean += deltaBeforeMeanUpdate / count;
        // Welford's trick
        double deltaAfterMeanUpdate = value - mean;
        sumSquaredDeltas += deltaBeforeMeanUpdate * deltaAfterMeanUpdate;
    }

    public long getCount() {
        return count;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return count < 2 ? 0.0 : sumSquaredDeltas / (count - 1);
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }
}
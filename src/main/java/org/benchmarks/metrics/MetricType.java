package org.benchmarks.metrics;

import org.HdrHistogram.AbstractHistogram;

public enum MetricType {
    COUNTS,
    P0_VALUES,
    P50_VALUES,
    P90_VALUES,
    P99_VALUES,
    P999_VALUES,
    P9999_VALUES,
    P100_VALUES,
    THROUGHPUT,
    ;

    public double getValue(AbstractHistogram histogram, double latencyFactor) {
        switch (this) {
        case COUNTS:
            return histogram.getTotalCount();
        case P0_VALUES:
            return histogram.getMinValue() / latencyFactor;
        case P50_VALUES:
            return histogram.getValueAtPercentile(50) / latencyFactor;
        case P90_VALUES:
            return histogram.getValueAtPercentile(90) / latencyFactor;
        case P99_VALUES:
            return histogram.getValueAtPercentile(99) / latencyFactor;
        case P999_VALUES:
            return histogram.getValueAtPercentile(99.9) / latencyFactor;
        case P9999_VALUES:
            return histogram.getValueAtPercentile(99.99) / latencyFactor;
        case P100_VALUES:
            return histogram.getMaxValue() / latencyFactor;
        case THROUGHPUT:
            long delay = histogram.getEndTimeStamp() - histogram.getStartTimeStamp();
            return delay > 0 ? histogram.getTotalCount() * 1000.0 / delay : -1;
        default:
            return -1;
        }
    }
}

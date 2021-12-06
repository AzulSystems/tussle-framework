package org.benchmarks.metrics;

import lombok.Data;

@Data
public class MetricValue {
    protected String type;
    protected String units;
    protected Double value;
    protected double[] values;

    public MetricValue() {
    }

    public MetricValue(String type, double[] values) {
        this.type = type;
        this.values = values;
    }

    public MetricValue(String type, double value) {
        this.type = type;
        this.value = value;
    }

    public double maxValue() {
        double res = -1;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (res < values[i]) {
                    res = values[i];
                }
            }
        }
        return res;
    }

    public double avgValue() {
        double sum = 0;
        int count = 0;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] > 0) {
                    sum += values[i];
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : -1;
    }

    public double sumValue() {
        double sum = 0;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] > 0) {
                    sum += values[i];
                }
            }
        }
        return sum;
    }
}
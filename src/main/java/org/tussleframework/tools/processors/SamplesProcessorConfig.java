package org.tussleframework.tools.processors;

import org.tussleframework.AbstractConfig;

import lombok.Data;

@Data
public class SamplesProcessorConfig implements AbstractConfig {

    public String name = "data";
    public int interval = 5000;
    public double timestampFactor = 1000;
    public double histogramFactor = 1000;
    public boolean hasHeader = true;
    public double[] percentiles = { 0, 50, 90, 99, 99.9, 99.99, 100 };

    @Override
    public void validate(boolean runMode) {
        if (percentiles == null || percentiles.length == 0) {
            throw new IllegalArgumentException("Missing percentiles");
        }
        for (int i = 0; i < percentiles.length; i++) {
            if (percentiles[i] < 0 || percentiles[i] > 100) {
                throw new IllegalArgumentException(String.format("Invalid percentile %f at index %d", percentiles[i], i));
            }
        }
    }
}

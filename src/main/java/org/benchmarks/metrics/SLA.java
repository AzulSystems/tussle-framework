package org.benchmarks.metrics;

import static org.benchmarks.tools.FormatTool.format;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SLA {
    public double percentile;
    public double maxValue;
    public int movingWindow;

    public String toString() {
        return "p" + percentile + " percentile = " + maxValue + "ms in " + movingWindow;
    }

    // Format:
    // p50-sla1ms-mw10s
    public String longName() {
        return "p" + format(percentile) + "-sla" + format(maxValue) + "ms-mw" + (movingWindow) + "s";
    }

    // Format:
    // p50-sla1ms
    public String withMaxName() {
        return "p" + format(percentile) + "-sla" + format(maxValue) + "ms";
    }

    // Format:
    // p50-mw10s
    public String withMWName() {
        return "p" + format(percentile) + "-mw" + (movingWindow) + "s";
    }
}

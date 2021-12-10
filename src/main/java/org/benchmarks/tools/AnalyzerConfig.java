package org.benchmarks.tools;

import org.benchmarks.metrics.Interval;
import org.benchmarks.metrics.SLA;

import lombok.Data;

@Data
public class AnalyzerConfig {
    public int mergeHistos = 1;
    public boolean doc = true;
    public boolean makeReport = true;
    public boolean allPercentiles = true;
    public String resultsDir = "./results";
    public String reportDir = "./report";
    public String highBound = "0";
    public SLA[] slaConfig = {};
    public Interval[] intervals = {};
}

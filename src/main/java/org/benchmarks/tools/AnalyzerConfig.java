package org.benchmarks.tools;

import org.benchmarks.metrics.Interval;
import org.benchmarks.metrics.SLA;

import lombok.Data;

@Data
public class AnalyzerConfig {
    public SLA[] slaConfig = {};
    public Interval[] intervals = {};
    public String resultsDir = "./results";
    public String reportDir = "./report";
    public int highBound = 0;
    public int mergeHistos = 1;
    public boolean doc = true;
    public boolean makeReport = true;
    public boolean longPercentiles = true;
}

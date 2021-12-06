package org.benchmarks;

import org.benchmarks.tools.FormatTool;

import lombok.Data;

/**
 * Basic benchmark configuration 
 * 
 */
@Data
public class BenchmarkConfig {
    public String targetRate = "1k";   // op/s, expected target throughput
    public String warmupTime = "0";    // sec, test warmup time
    public String runTime = "60";      // sec, test run time
    public int runSteps = 1;           // number of run steps
    public int intervalLength = 1000;  // ms, histogram write interval length
    public int progressIntervals = 5;  // ms, output progress interval count
    public double histogramFactor = 1000000; // conversion factor (divider) from histogram's units to milliseconds, e.g. for ns-to-ms it is 1000000 
    public boolean reset = true;       // call reset before each benchmark run by 'Runner'
    public boolean makeReport = false; // generate detailed report in addition to the summary results printed to log
    public boolean rawData = false;    // collect raw data: each request start and finish time stamps
    public String histogramsDir = "./histograms"; // location for histogram files
    public String reportDir = "./report";         // location for report files

    public void validate() {
        if (FormatTool.parseValue(targetRate) < 0) {
            throw new IllegalArgumentException(String.format("Invalid targetRate(%s) - should be non-negative", targetRate));
        }
        if (FormatTool.parseTimeLength(warmupTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid warmupTime(%s) - should be non-negative", warmupTime));
        }
        if (FormatTool.parseTimeLength(runTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid runTime(%s) - should be non-negative", runTime));
        }
        if (FormatTool.parseTimeLength(warmupTime) + FormatTool.parseTimeLength(runTime) <= 0) {
            throw new IllegalArgumentException(String.format("Invalid warmupTime(%s) or runTime(%s) - sum should be positive", warmupTime, runTime));
        }
        if (histogramsDir == null) {
            throw new IllegalArgumentException("Invalid histogramsDir - null");
        }
    }
}

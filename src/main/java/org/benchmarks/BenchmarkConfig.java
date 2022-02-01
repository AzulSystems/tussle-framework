/*
 * Copyright (c) 2021, Azul Systems
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of [project] nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package org.benchmarks;

import java.io.File;

import org.benchmarks.tools.FileTool;
import org.benchmarks.tools.FormatTool;

import lombok.Data;

/**
 * Basic benchmark configuration
 * 
 */
@Data
public class BenchmarkConfig implements AbstractConfig {
    public int runSteps = 1; // number of run steps
    public int intervalLength = 1000; // ms, histogram write interval length
    public int progressIntervals = 5; // ms, output progress interval count
    public double histogramFactor = 1000000; // histogram's units divider to milliseconds, e.g. for ns-to-ms it is 1000000
    public boolean reset = true; // reset benchmark before run in the Runner scenario
    public boolean rawData = false; // collect each request raw data: start and finish times
    public boolean makeReport = false; // generate detailed report in addition to the summary results printed to log
    public String runTime = "60"; // sec, test run time
    public String targetRate = "1k"; // op/s, expected target throughput
    public String warmupTime = "0"; // sec, test warmup time
    public String reportDir = "./report"; // location for report files
    public String histogramsDir = "./histograms"; // location for histogram (hdr) files

    @Override
    public void validate(boolean runMode) {
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
        if (runMode) {
            createDirs();
        }
    }

    public void createDirs() {
        File histogramsDirFile = new File(histogramsDir);
        File reportDirFile = new File(reportDir);
        if (FileTool.isFileOrNonEmptyDir(histogramsDirFile)) {
            throw new IllegalArgumentException(String.format("Non-empty histograms dir '%s' already exists", histogramsDirFile));
        }
        if (makeReport && FileTool.isFileOrNonEmptyDir(reportDirFile)) {
            throw new IllegalArgumentException(String.format("Non-empty report dir '%s' already exists", reportDirFile));
        }
        if (!histogramsDirFile.exists() && !histogramsDirFile.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create histograms dir '%s'", histogramsDirFile));
        }
        if (makeReport && !reportDirFile.exists() && !reportDirFile.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create report dir '%s'", reportDirFile));
        }
    }

}

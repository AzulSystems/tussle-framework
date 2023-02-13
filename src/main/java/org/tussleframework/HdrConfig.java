/*
 * Copyright (c) 2021-2023, Azul Systems
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

package org.tussleframework;

import java.util.regex.Pattern;

import org.tussleframework.metrics.HdrResult;
import org.tussleframework.tools.JsonTool;

/**
 * Basic benchmark configuration
 */
public class HdrConfig implements AbstractConfig {
    public int hdrCutTime = 0;                    // initial hdr records to be excluded during this time in seconds
    public int hdrInterval = 1000;                // time interval in milliseconds used for writing histogram bunch of compressed data to hdr files 
    public int reportInterval = 3000;             // time interval in milliseconds used for reporting histogram results
    public int progressInterval = 5000;           // time interval in milliseconds used for writing progress in the log output
    public double hdrFactor = 1000d;              // histogram's units divider to milliseconds, e.g. for ns-to-ms it is 1000000
    public boolean includeWarmup = false;
    public String histogramsDir = "./histograms"; // location for histogram (hdr) files
    public String metricName = HdrResult.SERVICE_TIME;
    public String rateUnits = "op/s";
    public String timeUnits = "ms";
    public String[] operationsInclude;
    public String[] operationsExclude;
    public RunProperties runProperties;
    public String runPropertiesFile;

    public HdrConfig copy(HdrConfig c) {
        hdrCutTime = c.hdrCutTime;
        hdrInterval = c.hdrInterval; 
        reportInterval = c.reportInterval;
        progressInterval = c.progressInterval;
        hdrFactor = c.hdrFactor;
        includeWarmup = c.includeWarmup;
        histogramsDir = c.histogramsDir;
        metricName = c.metricName;
        rateUnits = c.rateUnits;
        timeUnits = c.timeUnits;
        operationsInclude = c.operationsInclude;
        operationsExclude = c.operationsExclude;
        runProperties = c.runProperties;
        runPropertiesFile = c.runPropertiesFile;
        return this;
    }

    @Override
    public void validate(boolean runMode) {
        if (hdrCutTime < 0) {
            throw new IllegalArgumentException(String.format("Invalid hdrCutTime(%d) - should be >= 0", hdrCutTime));   
        }
        if (histogramsDir == null) {
            throw new IllegalArgumentException("Invalid histogramsDir - null");
        }
        if (hdrInterval < 10) {
            throw new IllegalArgumentException(String.format("Invalid hdrInterval(%d) - should be >= 10", hdrInterval));
        }
        if (hdrFactor <= 0) {
            throw new IllegalArgumentException(String.format("Invalid hdrFactor(%d) - should be positive", hdrFactor));
        }
        if (reportInterval < hdrInterval) {
            throw new IllegalArgumentException(String.format("Invalid reportInterval(%d) - should be >= hdrInterval(%d)", reportInterval, hdrInterval));
        }
        if (progressInterval < 0) {
            throw new IllegalArgumentException(String.format("Invalid progressInterval(%d) - should be >= 0", progressInterval));
        }
        if (operationsInclude != null) {
            for (String pattern : operationsInclude) {
                Pattern.compile(pattern);
            }
        }
        if (operationsExclude != null) {
            for (String pattern : operationsExclude) {
                Pattern.compile(pattern);
            }
        }
    }

    public void saveRunArgs(RunArgs runArgs) throws TussleException {
        JsonTool.printJson(runArgs, String.format("%s/run-args-%d.json", histogramsDir, runArgs.runStep));
    }

    public RunArgs loadRunArgs(int runStep) throws TussleException {
        return JsonTool.readJson(String.format("%s/run-args-%d.json", histogramsDir, runStep), RunArgs.class);
    }
}

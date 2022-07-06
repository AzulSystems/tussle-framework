/*
 * Copyright (c) 2021-2022, Azul Systems
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

package org.tussleframework.tools.processors;

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Logger;

import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.tools.LoggerTool;

public class SamplesProcessor implements DataLogProcessor {

    private SamplesProcessorConfig config;
    private boolean samples2 = false; 

    public SamplesProcessor() {
        this(new SamplesProcessorConfig());
    }

    public SamplesProcessor(SamplesProcessorConfig config) {
        this.config = config;
    }

    protected void log(String format, Object... args) {
        LoggerTool.log(getClass().getSimpleName(), format, args);
    }

    /**
     Samples format 1: 
     timestamp(s),value,..
     1618204400.390739,48,..
     1618204401.727753,34,..
     1618204403.057668,40,..
     1618204404.391447,45,..
     ...
     Samples format 2:
     1655401396.792424,0.013333,[25 pages],437.485229,True,W,439.595204
     1655401399.414480,0.016667,[25 pages],560.169258,True,W,561.651193
     1655401401.736531,0.020000,[25 pages],383.068766,True,W,383.702291
     1655401404.236440,0.023333,[25 pages],382.301198,True,W,383.610885
     1655402071.764652,0.913333,[25 pages],410.433045,True,M,411.823607
     1655402074.282562,0.916667,[25 pages],427.558961,True,M,429.733414
     1655402076.824134,0.920000,[25 pages],469.141951,True,M,471.305798
     ...
     */
    @Override
    public boolean processData(MetricData metricData, HdrData hdrData, InputStream inputStream, String host, Logger logger) {
        /// log("Config: %s", new Yaml().dump(config).trim())
        int stampsIdx = 0;
        int valuesIdx = 1;
        String firstLine = null;
        try (Scanner scanner = new Scanner(inputStream)) {
            if (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length > 5 &&
                        (parts[4].equalsIgnoreCase("true") || parts[4].equalsIgnoreCase("false")) && 
                        (parts[5].equalsIgnoreCase("W") || parts[5].equalsIgnoreCase("M"))) {
                    samples2 = true;
                    firstLine = line;
                } else if (config.hasHeader) {
                    for (int i = 0; i < parts.length; i++) {
                        String h = parts[i].toLowerCase();
                        if (h.indexOf("stamp") > 0) {
                            stampsIdx = i;
                        } else if (h.indexOf("value") > 0) {
                            valuesIdx = i;
                        }
                    }
                } else {
                    firstLine = line;
                }
            }
            if (samples2) {
                processSamples2(hdrData, scanner, firstLine);
            } else {
                processSamples(hdrData, scanner, firstLine, stampsIdx, valuesIdx);
            }
            return true;
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
    }

    /*
     Samples format 1: 
     timestamp(s),value,..
     1618204400.390739,48,..
     */
    protected void processSample(HdrData hdrData, String line, int stampsIdx, int valuesIdx) {
        if (line == null) {
            return;
        }
        String[] parts = line.split(",");
        long stamp = Math.round(Double.valueOf(parts[stampsIdx]) * config.timestampFactor);
        long value = Long.parseLong(parts[valuesIdx]);
        hdrData.recordValues(stamp, value, -1);
    }

    /*
     Samples format 2:
     1655401396.792424,0.013333,[25 pages],437.485229,True,W,439.595204
     1655402056.815843,0.893333,[25 pages],460.947098,True,M,463.013381
     */
    protected void processSample2(HdrData hdrData, String line) {
        if (line == null) {
            return;
        }
        String[] parts = line.split(",");
        boolean isWarmup = parts.length > 5 && (parts[5].equalsIgnoreCase("w") || parts[5].equalsIgnoreCase("warmup"));
        if (isWarmup) {
            return;
        }
        long stamp = Math.round(Double.valueOf(parts[0]) * config.timestampFactor);
        double serviceTime = Double.parseDouble(parts[3]); // from double in milliseconds
        double latencyValue = parts.length > 6 ? Double.parseDouble(parts[6]) : 0; // from double in milliseconds
        hdrData.recordValues(stamp, Math.round(serviceTime * 1000), Math.round(latencyValue * 1000)); // to long microseconds
    }

    protected void processSamples(HdrData hdrData, Scanner scanner, String firstLine, int stampsIdx, int valuesIdx) {
        log("processSamples...");
        processSample(hdrData, firstLine, stampsIdx, valuesIdx);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            processSample(hdrData, line, stampsIdx, valuesIdx);
        }
    }

    protected void processSamples2(HdrData hdrData, Scanner scanner, String firstLine) {
        log("processSamples2...");
        processSample2(hdrData, firstLine);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            processSample2(hdrData, line);
        }
    }
}

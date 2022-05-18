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

package org.tussleframework;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogReader;
import org.tussleframework.metrics.Interval;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.metrics.ServiceLevelExpectation;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class HdrResult {

    public String operationName;
    public String metricName;
    public String hdrFile;
    public String rateUnits;
    public String timeUnits;
    public RunArgs runArgs;
    public double actualRate;
    public double histogramFactor;
    public int intervalLength;
    public int recordsCount;
    public Histogram allHistogram = new Histogram(3);
    public List<HdrIntervalResult> subIntervalHistograms = new ArrayList<>();

    private static String responseTime = "response_time";
    private static String serviceTime = "service_time";
    private static String responseTime2 = "response-time";
    private static String serviceTime2 = "service-time";
    
    public HdrResult() {
        ///
    }

    public HdrResult(String operationName, String metricName, String rateUnits, String timeUnits, String hdrFile, RunArgs runArgs, double actualRate, RunnerConfig config) {
        this.operationName = operationName;
        this.metricName = metricName;
        this.rateUnits = rateUnits;
        this.timeUnits = timeUnits;
        this.hdrFile = hdrFile;
        this.runArgs = runArgs;
        this.actualRate = actualRate;
        this.histogramFactor  = config.histogramFactor;
        this.intervalLength = config.intervalLength;
    }

    public void detectValues(String fileName, String defOperationName) {
        String[] parts = fileName.replace(serviceTime, serviceTime2).replace(responseTime, responseTime2).split("_");
        int nums = 0;
        runArgs = new RunArgs();
        try {
            runArgs.step = Integer.valueOf(parts[parts.length - 1]);
            runArgs.targetRate = Double.valueOf(parts[parts.length - 2]);
            runArgs.ratePercent = Double.valueOf(parts[parts.length - 3]);
            nums = 3;
        } catch (NumberFormatException e) {
            try {
                runArgs.step = Integer.valueOf(parts[parts.length - 1]);
                runArgs.targetRate = 0;
                runArgs.ratePercent = Double.valueOf(parts[parts.length - 2]);
                nums = 2;
            } catch (NumberFormatException e2) {
                runArgs.step = 0;
                runArgs.targetRate = 0;
                runArgs.ratePercent = 0;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - nums; i++) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(parts[i]);
        }
        metricName = sb.toString();
        int pos = metricName.indexOf('_');
        if (pos > 0) {
            operationName = metricName.substring(0, pos);
            metricName = metricName.substring(pos + 1);
        } else {
            operationName = defOperationName;
        }
        metricName = metricName.replace(serviceTime2, serviceTime).replace(responseTime2, responseTime);
    }

    public String getOpName() {
        return String.format("%s_%s_%s_%d", operationName, FormatTool.roundFormatPercent(runArgs.ratePercent), FormatTool.format(runArgs.targetRate), runArgs.step);
    }

    public static String clearPathAndExtension(String fileName) {
        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            fileName = fileName.substring(pos + 1);
        }
        pos = fileName.lastIndexOf('.');
        if (pos >= 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    public static HdrResult getIterationResult(String fileName, String defOperationName) {
        HdrResult result = new HdrResult();
        result.hdrFile = fileName;
        result.intervalLength = 1000;
        final String hs = ".hdr-";
        if (fileName.startsWith("tlp_stress_metrics") && fileName.indexOf(hs) >= 0) {
        	result.runArgs = new RunArgs();
            result.histogramFactor = 1000;
            result.operationName = fileName.substring(fileName.indexOf(hs) + hs.length());
            if (result.operationName.startsWith("INTENDED-")) {
                result.operationName = result.operationName.substring("INTENDED-".length());
                result.metricName = responseTime;
            } else {
                result.metricName = serviceTime;
            }
        } else {
            result.histogramFactor = 1000_000;
            result.detectValues(clearPathAndExtension(fileName), defOperationName);
        }
        return result;
    }

    public void processHistograms(MetricData metricData, InputStream inputStream, MovingWindowSLE[] sleConfig, Interval[] intervals, double[] percentiles, int mergeHistos) {
        LoggerTool.log("HdrResult", "processHistogram '%s', operation %s, metricName %s, merge %d adjucent histograms", hdrFile, operationName, metricName, mergeHistos);
        recordsCount = 0;
        try (HistogramLogReader hdrReader = new HistogramLogReader(inputStream)) {
            int nulls = 0;
            double rangeStartTimeSec = 0.0;
            double rangeEndTimeSec = Double.MAX_VALUE;
            subIntervalHistograms = new ArrayList<>();
            for (Interval interval : intervals) {
                subIntervalHistograms.add(new HdrIntervalResult(sleConfig, interval, histogramFactor));
            }
            Histogram firstHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            if (firstHistogram == null) {
                return;
            }
            final long stampStart = firstHistogram.getStartTimeStamp();
            subIntervalHistograms.forEach(sh -> sh.adjustInterval(stampStart));
            while (true) {
                ArrayList<AbstractHistogram> histos = new ArrayList<>();
                int hi = 0;
                if (firstHistogram != null) {
                    hi = 1;
                    histos.add(firstHistogram);
                    firstHistogram = null;
                }
                while (hi < mergeHistos) {
                    AbstractHistogram intervalHistogram = (AbstractHistogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
                    if (intervalHistogram != null) {
                        histos.add(intervalHistogram);
                    }
                    hi++;
                }
                if (!histos.isEmpty()) {
                    recordsCount++;
                    subIntervalHistograms.forEach(sh -> sh.add(histos));
                } else if (nulls++ > 10) {
                    break;
                }
            }
            subIntervalHistograms.forEach(sh -> sh.processHistogram(this, metricData, percentiles, mergeHistos));
        }
    }

    public boolean checkSLE(ServiceLevelExpectation aSLE) {
        if (!(aSLE instanceof MovingWindowSLE)) {
            return false;
        }
        MovingWindowSLE mwSLE = (MovingWindowSLE) aSLE;
        try (HistogramLogReader hdrReader = new HistogramLogReader(hdrFile)) {
            double rangeStartTimeSec = 0.0;
            double rangeEndTimeSec = Double.MAX_VALUE;
            Queue<Histogram> movingWindowQueue = new LinkedList<>();
            Histogram intervalHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            Histogram movingWindowSumHistogram = new Histogram(3);
            while (intervalHistogram != null) {
                long windowCutOffTimeStamp = intervalHistogram.getEndTimeStamp() - mwSLE.movingWindow * 1000;
                movingWindowSumHistogram.add(intervalHistogram);
                Histogram head = movingWindowQueue.peek();
                while (head != null && head.getEndTimeStamp() <= windowCutOffTimeStamp) {
                    Histogram prevHist = movingWindowQueue.remove();
                    movingWindowSumHistogram.subtract(prevHist);
                    head = movingWindowQueue.peek();
                }
                movingWindowQueue.add(intervalHistogram);
                if (movingWindowSumHistogram.getValueAtPercentile(mwSLE.percentile) / histogramFactor > mwSLE.maxValue) {
                    return false;
                }
                intervalHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            }
        } catch (Exception e) {
            LoggerTool.logException(null, e);
            return true;
        }
        return true;
    }
}

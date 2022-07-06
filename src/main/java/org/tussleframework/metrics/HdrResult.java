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

package org.tussleframework.metrics;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogReader;
import org.tussleframework.HdrConfig;
import org.tussleframework.RunArgs;
import org.tussleframework.RunResult;
import org.tussleframework.TussleException;
import org.tussleframework.tools.FileTool;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

interface HdrIterator {
    AbstractHistogram next();
}

interface HdrIteratorSource {
    HdrIterator get();
}

public class HdrResult {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HdrResult.class.getName());

    private static String responseTime = "response_time";
    private static String serviceTime = "service_time";
    private static String responseTime2 = "response-time";
    private static String serviceTime2 = "service-time";
    private static String intendedPref = "intended-";

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", HdrResult.class.getSimpleName(), String.format(format, args)));
        }
    }

    public static RunResult getSummaryResult(Collection<HdrResult> hdrResults) {
        RunResult runResult = new RunResult();
        HashMap<String, HdrResult> hdrMap = new HashMap<>();
        hdrResults.forEach(hdr -> hdrMap.put(hdr.operationName(), hdr));
        for (HdrResult hdrResult : hdrMap.values()) {
            runResult.rate += hdrResult.getRate();
            runResult.count += hdrResult.getCount();
            if (runResult.time < hdrResult.getTimeMs()) {
                runResult.time = hdrResult.getTimeMs();
            }
        }
        return runResult;
    }

    protected List<HdrIntervalResult> hdrIntervalResults = new ArrayList<>();
    protected MetricInfo metricInfo;
    protected HdrConfig config;
    protected RunArgs runArgs;
    protected String hdrFile;
    protected int recordsCount;

    public HdrResult() {
        ///
    }

    public HdrResult(MetricInfo metricInfo, String hdrFile, RunArgs runArgs, HdrConfig config) {
        this.metricInfo = metricInfo;
        this.config = new HdrConfig().copy(config);
        this.hdrFile = hdrFile;
        this.runArgs = runArgs;
        hdrIntervalResults.add(new HdrIntervalResult(new Interval(), this.config, null));
    }

    public HdrResult(String fileName, HdrConfig config) {
        this.metricInfo = new MetricInfo();
        this.hdrFile = fileName;
        this.config = new HdrConfig().copy(config);
        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            fileName = fileName.substring(pos + 1);
        }
        final String hs = ".hdr-";
        if (fileName.startsWith("tlp_stress_metrics") && fileName.indexOf(hs) >= 0) {
            this.runArgs = new RunArgs();
            this.config.hdrFactor = 1000;
            String operationName = fileName.substring(fileName.indexOf(hs) + hs.length());
            if (operationName.toLowerCase().startsWith(intendedPref)) {
                operationName = operationName.substring(intendedPref.length());
                metricInfo.metricName = responseTime;
            } else {
                metricInfo.metricName = serviceTime;
            }
            metricInfo.operationName = operationName;
        } else {
            /*
             * @param fileName -> file name formats based on underscore ('_') separator:
             *      {operation-name}_{metric-name}_{numeric-percent-of-high-bound}_{numeric-target-rate}_{numeric-step}
             *      {operation-name}_{metric-name}_{numeric-percent-of-high-bound}_{numeric-step}
             *      {operation-name}_{metric-name}_{numeric-step}
             *      {operation-name}_{metric-name}
             *      {operation-name}_{percent-of-high-bound}_{numeric-target-rate}_{numeric-step}
             *      {operation-name}_{percent-of-high-bound}_{numeric-step}
             *      {operation-name}_{numeric-step}
             *      {operation-name}
             *      
             * Example names:
             *      READS_response-time-100_20000_0.hlog
             *      WRITES_response-time-100_80000_0.hlog
             *      
             *      writes_service-time_0.hlog
             *      writes_service-time_1.hlog
             *      writes_service-time_2.hlog
             *      writes_service-time_3.hlog
             *      
             * @param defaultName
             */
            fileName = FileTool.clearPathAndExtension(fileName);
            String[] parts = fileName.replace(serviceTime, serviceTime2).replace(responseTime, responseTime2).split("_");
            runArgs = new RunArgs();
            int filledParts = runArgs.fillValues(parts);
            metricInfo.fillValues(parts, filledParts, config.metricName);
        }
        hdrIntervalResults.add(new HdrIntervalResult(new Interval(), this.config, null));
    }

    public void setRunArgs(RunArgs runArgs) {
        this.runArgs = runArgs;
    }

    public String operationName() {
        return metricInfo.operationName;
    }

    public String metricName() {
        return metricInfo.metricName;
    }

    public String rateUnits() {
        return metricInfo.rateUnits;
    }

    public String timeUnits() {
        return metricInfo.timeUnits;
    }

    public String hdrFile() {
        return hdrFile;
    }

    public double targetRate() {
        return runArgs.targetRate;
    }

    public int runTime() {
        return runArgs.runTime;
    }

    public int warmupTime() {
        return runArgs.warmupTime;
    }

    public int step() {
        return runArgs.runStep;
    }

    public int recordsCount() {
        return recordsCount;
    }

    /**
     * Return full interval histogram
     */
    public HdrIntervalResult getPrimeResult() {
        return hdrIntervalResults.get(0);
    }

    public Histogram getPrimeHistogram() {
        return getPrimeResult().getHistogram();
    }

    public double getRate() {
        return getPrimeResult().getRate();
    }

    public double getMean() {
        return getPrimeResult().getMean();
    }

    public double getMaxValue() {
        return getPrimeResult().getMaxValue();
    }

    public long getTimeMs() {
        return getPrimeResult().getTimeMs();
    }

    public double getValueAtPercentile(double percentile) {
        return getPrimeResult().getValueAtPercentile(percentile);
    }

    public long getCount() {
        return getPrimeResult().getCount();
    }

    public double hdrFactor() {
        return config.hdrFactor;
    }

    public void getMetrics(MetricData metricData, double[] percentiles) {
        hdrIntervalResults.forEach(sh -> sh.getMetrics(this, metricData, percentiles));
    }

    public void add(AbstractHistogram inputHistogram) {
        getPrimeResult().addHistogram(inputHistogram);
    }

    public String getOpName() {
        return String.format("%s_%s_%s_%d", metricInfo.operationName, FormatTool.roundFormatPercent(runArgs.ratePercent), FormatTool.format(runArgs.targetRate), runArgs.runStep);
    }

    public void loadHdrFile(MovingWindowSLE[] sleConfig, Interval[] intervals) throws TussleException {
        log("Loading from HDR file '%s'", hdrFile);
        try (InputStream inputStream = new FileInputStream(hdrFile)) {
            loadHdrData(inputStream, sleConfig, intervals);
        } catch (Exception e) {
            throw new TussleException(e);
        }
    }

    public void loadHdrData(InputStream inputStream, MovingWindowSLE[] sleConfig, Interval[] intervals) {
        try (HistogramLogReader hdrReader = new HistogramLogReader(inputStream)) {
            loadHdrData(() -> (AbstractHistogram) hdrReader.nextIntervalHistogram(0.0, Double.MAX_VALUE), sleConfig, intervals);
        }
    }

    public void loadHdrData(HdrIterator hdrIter, MovingWindowSLE[] sleConfig, Interval[] intervals) {
        int mergeHistos = config.reportInterval / config.hdrInterval;
        log("Loading HDR: operation %s, metricName %s, hdrFactor %s, (reportInterval %d ms) / (hdrInterval %d ms) = (histograms per reportInterval %d)", 
                metricInfo.operationName, metricInfo.metricName, FormatTool.format(config.hdrFactor), config.reportInterval, config.hdrInterval, mergeHistos);
        if (intervals == null || intervals.length == 0) {
            intervals = new Interval[] { new Interval() };
        }
        hdrIntervalResults = new ArrayList<>();
        for (Interval interval : intervals) {
            hdrIntervalResults.add(new HdrIntervalResult(interval, config, sleConfig));
        }
        int nulls = 0;
        int histoCount = 0;
        recordsCount = 0;
        while (true) {
            ArrayList<AbstractHistogram> histos = new ArrayList<>();
            int histoIdx = 0;
            while (histoIdx < mergeHistos) {
                AbstractHistogram intervalHistogram = hdrIter.next();
                if (intervalHistogram != null) {
                    if (histoCount == 0) {
                        hdrIntervalResults.forEach(sh -> sh.adjustInterval(intervalHistogram.getStartTimeStamp()));
                    }
                    histos.add(intervalHistogram);
                    histoCount++;
                }
                histoIdx++;
            }
            if (!histos.isEmpty()) {
                recordsCount++;
                hdrIntervalResults.forEach(sh -> sh.addHistograms(histos));
            } else if (nulls++ > 10) {
                break;
            }
        }
        LoggerTool.log(getClass().getSimpleName(), "Loaded HDR records %d, hdrFactor %f", recordsCount, config.hdrFactor);
    }

    public boolean checkSLE(ServiceLevelExpectation aSLE) {
        if (!(aSLE instanceof MovingWindowSLE)) {
            return false;
        }
        try (HistogramLogReader hdrReader = new HistogramLogReader(hdrFile)) {
            return checkSLE(() -> (AbstractHistogram) hdrReader.nextIntervalHistogram(0.0, Double.MAX_VALUE), (MovingWindowSLE) aSLE);
        } catch (Exception e) {
            LoggerTool.logException(null, e);
            return true;
        }
    }

    public boolean checkSLE(HdrIterator hdrIter, MovingWindowSLE mwSLE) {
        MovingWindowHistogram mwHistogram = new MovingWindowHistogram(mwSLE, config.hdrFactor);
        AbstractHistogram intervalHistogram = hdrIter.next();
        while (intervalHistogram != null) {
            mwHistogram.add(intervalHistogram);
            if (!mwHistogram.checkSLE()) {
                return false;
            }
            intervalHistogram = hdrIter.next();
        }
        return true;
    }
}

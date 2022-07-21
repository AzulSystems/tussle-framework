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

import static org.tussleframework.Globals.*;

import static org.tussleframework.tools.FormatTool.matchFilters;
import static org.tussleframework.tools.FormatTool.roundFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.tussleframework.RunArgs;
import org.tussleframework.TimeRecorder;
import org.tussleframework.TussleException;
import org.tussleframework.TussleRuntimeException;
import org.tussleframework.runners.RunnerConfig;
import org.tussleframework.tools.Analyzer;
import org.tussleframework.tools.AnalyzerConfig;
import org.tussleframework.tools.LoggerTool;

public class ResultsRecorder implements TimeRecorder {

    public static long timeLen0(long startTime, long finishTime) {
        if (startTime >= 0) {
            return finishTime > startTime ? (finishTime - startTime) / NS_IN_US : 0;
        } else {
            return -1;
        }
    }

    public static long timeLen1(long startTime, long finishTime) {
        return startTime > 0 && finishTime > startTime ? (finishTime - startTime) / NS_IN_US : -1;
    }

    class OperationsRecorder {

        private OutputStream rawDataOutputStream;
        private HdrWriter responseTimeWriter;
        private HdrWriter serviceTimeWriter;
        private HdrWriter errorsWriter;
        private boolean serviceTimeOnly;
        private long startTime0;
        private int hdrInterval;

        public OperationsRecorder(MetricInfo metricInfo) throws IOException {
            serviceTimeOnly = runnerConfig.serviceTimeOnly;
            hdrInterval = runnerConfig.hdrInterval;
            responseTimeWriter = new HdrWriter(metricInfo.replaceMetricName(HdrResult.RESPONSE_TIME), writeHdr, runnerConfig.progressInterval, runArgs, runnerConfig, runnerConfig.histogramsDir);
            serviceTimeWriter = new HdrWriter(metricInfo.replaceMetricName(HdrResult.SERVICE_TIME), writeHdr, runnerConfig.progressInterval, runArgs, runnerConfig, runnerConfig.histogramsDir);
            errorsWriter = new HdrWriter(metricInfo.replaceMetricName("errors"), writeHdr, runnerConfig.progressInterval, runArgs, runnerConfig, runnerConfig.histogramsDir);
            if (runnerConfig.rawData) {
                String rawFile = String.format("%s/%s", runnerConfig.histogramsDir, metricInfo.replaceMetricName("samples-data").formatFileName(runArgs, "raw"));
                rawDataOutputStream = new BufferedOutputStream(new FileOutputStream(new File(rawFile)), 128 * 1024 * 1024);
                startTime0 = System.nanoTime() + NANO_TIME_OFFSET;
                rawDataOutputStream.write(String.format("# abs start time: %d ms since ZERO%n", startTime0 / NS_IN_MS).getBytes());
                rawDataOutputStream.write(String.format("start_time(us),intended_start_time(us),finish_time(us),count,finish_time-start_time(us),finish_time-intended_start_time(us),thread_name%n").getBytes());
            }
        }

        void recordTimes(long startTime, long intendedStartTime, long finishTime, long count, boolean success) {
            if (success) {
                if (startTime > 0) {
                    serviceTimeWriter.recordTime(timeLen1(startTime, finishTime), count);
                }
                if (intendedStartTime > 0 && !serviceTimeOnly) {
                    responseTimeWriter.recordTime(timeLen1(intendedStartTime, finishTime), count);
                }
            } else {
                errorsWriter.recordTime(timeLen1(startTime, finishTime), count);
            }
            OutputStream rawStream = this.rawDataOutputStream;
            if (rawStream != null) {
                try {
                    rawStream.write(String.format(intendedStartTime > 0 ? "%06d,%06d,%d,%d,%d,%d,%s%n" : "%06d,%d,%d,%d,%d,%d,%s%n"
                            , timeLen0(startTime0, startTime)
                            , timeLen0(startTime0, intendedStartTime)
                            , timeLen0(startTime0, finishTime)
                            , count
                            , timeLen1(startTime, finishTime)
                            , timeLen1(intendedStartTime, finishTime)
                            , Thread.currentThread().getName()).getBytes());
                } catch (IOException e) {
                    LoggerTool.logException(null, e);
                    try {
                        rawStream.close();
                    } catch (IOException e2) {
                        ///
                    }
                    this.rawDataOutputStream = null;
                }
            }
        }

        void startRecording(Timer timer, long startTime) {
            responseTimeWriter.recordingStarted(startTime);
            serviceTimeWriter.recordingStarted(startTime);
            errorsWriter.recordingStarted(startTime);
            timer.scheduleAtFixedRate(responseTimeWriter, hdrInterval, hdrInterval);
            timer.scheduleAtFixedRate(serviceTimeWriter, hdrInterval, hdrInterval);
            timer.scheduleAtFixedRate(errorsWriter, hdrInterval, hdrInterval);
        }

        void cancel() {
            OutputStream rawStream = this.rawDataOutputStream;
            this.rawDataOutputStream = null;
            if (rawStream != null) {
                try {
                    rawStream.close();
                } catch (IOException e) {
                    ///
                }
            }
            responseTimeWriter.cancel();
            serviceTimeWriter.cancel();
            errorsWriter.cancel();
        }

        void getResults(Collection<HdrResult> hdrResults) {
            if (hdrResults == null) {
                return;
            }
            if (!responseTimeWriter.isEmpty()) {
                hdrResults.add(responseTimeWriter.getHdrResult());
            }
            if (!serviceTimeWriter.isEmpty()) {
                hdrResults.add(serviceTimeWriter.getHdrResult());
            }
            if (!errorsWriter.isEmpty()) {
                hdrResults.add(errorsWriter.getHdrResult());
            }
        }
    }

    private Collection<HdrResult> hdrResults = new ArrayList<>();
    private Map<String, OperationsRecorder> recordingsMap = new HashMap<>();
    private Set<String> recordingsFilter = new HashSet<>();
    private Timer timer = new Timer();
    private RunnerConfig runnerConfig;
    private RunArgs runArgs;
    private boolean writeHdr;
    private boolean cancelOnStop;

    @Override
    public void startRecording(String operationName, String rateUnits, String timeUnits) {
        if (!recordingsFilter.isEmpty() && !recordingsFilter.contains(operationName)) {
            return;
        }
        if (recordingsMap.containsKey(operationName)) {
            return;
        }
        try {
            OperationsRecorder opRecorder = new OperationsRecorder(new MetricInfo(operationName, null, rateUnits, timeUnits, null));
            recordingsMap.put(operationName, opRecorder);
            opRecorder.startRecording(timer, System.currentTimeMillis());
        } catch (IOException e) {
            throw new TussleRuntimeException(e);
        }
    }

    @Override
    public void stopRecording() {
        if (cancelOnStop) {
            cancel();
        }
    }

    @Override
    public void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, long count, boolean success) {
        OperationsRecorder opRecorder = recordingsMap.get(operation);
        if (opRecorder != null) {
            opRecorder.recordTimes(startTime, intendedStartTime, finishTime, count, success);
        }
    }

    public ResultsRecorder(RunnerConfig runnerConfig, RunArgs runArgs, boolean writeHdr, boolean cancelOnStop) {
        this.runnerConfig = runnerConfig;
        this.runArgs = runArgs;
        this.writeHdr = writeHdr;
        this.cancelOnStop = cancelOnStop;
        if (runnerConfig.collectOps != null) {
            Collections.addAll(recordingsFilter, runnerConfig.collectOps);
        }
        HdrWriter.progressHeaderPrinted(false);
    }

    public void cancel() {
        timer.cancel();
        recordingsMap.forEach((s, r) -> r.cancel());
    }

    public Collection<HdrResult> getHdrResults() {
        return getHdrResults(new ArrayList<>());
    }

    public Collection<HdrResult> getHdrResults(Collection<HdrResult> hdrResults) {
        if (hdrResults != null) {
            recordingsMap.forEach((s, r) -> r.getResults(hdrResults));
            hdrResults.addAll(this.hdrResults);
        }
        return hdrResults;
    }

    public void clearResults() {
        recordingsMap.clear();
        hdrResults.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addResults(Collection<?> results, String rateUnits, String timeUnits) throws TussleException {
        if (results.isEmpty()) {
            return;
        }
        Object r = results.iterator().next();
        if (r instanceof File) {
            loadCustomHdrResult((Collection<File>) results, runArgs, rateUnits, timeUnits);
        } else if (r instanceof HdrResult) {
            results.forEach(result -> hdrResults.add((HdrResult) result));
        } else {
            throw new TussleException("Unsupported result type: " + r.getClass().getName());
        }
    }

    protected void loadCustomHdrResult(Collection<File> resultFiles, RunArgs runArgs, String rateUnits, String timeUnits) throws TussleException {
        if (resultFiles == null || resultFiles.isEmpty()) {
            return;
        }
        resultFiles.forEach(file -> LoggerTool.log(getClass().getSimpleName(), "Processing found result file '%s'", file));
        for (File resultFile : resultFiles) {
            Collection<HdrResult> hdrs = new ArrayList<>();
            if (Analyzer.isSamplesFile(resultFile.getName()) || Analyzer.isArchiveFile(resultFile.getName())) {
                loadHdrFromSamples(resultFile, hdrs);
            } else {
                HdrResult hdrResult = loadHdrFromFile(resultFile);
                if (hdrResult != null) {
                    hdrs.add(hdrResult);
                }
            }
            hdrs.forEach(hdr -> {
                hdr.setRunArgs(runArgs);
                hdr.metricInfo.rateUnits = rateUnits;
                hdr.metricInfo.timeUnits = timeUnits;
                hdrResults.add(hdr);
            });
        }
    }

    protected void loadHdrFromSamples(File resultFile, Collection<HdrResult> hdrs) throws TussleException {
        Analyzer analyzer = new Analyzer();
        AnalyzerConfig analyzerConfig = new AnalyzerConfig(runnerConfig);
        analyzerConfig.sleConfig = runnerConfig.sleConfig;
        analyzer.init(analyzerConfig);
        analyzer.currentRunArgs = runArgs;
        analyzer.processFile(resultFile);
        analyzer.saveHdrs();
        analyzer.getHdrResults(hdrs);
    }

    protected HdrResult loadHdrFromFile(File resultFile) throws TussleException {
        HdrResult hdrResult = new HdrResult(resultFile.getAbsolutePath(), runnerConfig);
        if (!matchFilters(hdrResult.operationName(), runnerConfig.operationsInclude, runnerConfig.operationsExclude)) {
            LoggerTool.log(getClass().getSimpleName(), " skipping operation %s", hdrResult.operationName());
            return null;
        }
        hdrResult.loadHdrFile(null, null);
        LoggerTool.log(getClass().getSimpleName(), "Loadied HDR from file: '%s', %s %s, rate %s %s", resultFile, hdrResult.operationName(), hdrResult.metricName(), roundFormat(hdrResult.getRate()), hdrResult.rateUnits());
        return hdrResult;
    }
}

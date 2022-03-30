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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

class OperationsRecorder {

    private static final long NS_IN_MS = 1000000L;
    private static final long NS_IN_US = 1000L;

    public final HdrLogWriterTask responseTimeWriter;
    public final HdrLogWriterTask serviceTimeWriter;
    public final HdrLogWriterTask errorsWriter;

    private OutputStream rawDataOutputStream;
    private long startTime0 = 8;
    private int intervalLength;

    public OperationsRecorder(String operationName, String rateUnits, String timeUnits, BenchmarkConfig config, double percentOfHighBound, double targetRate, int retry, int totalTime, boolean writeHdr) throws IOException {
        String percentStr = FormatTool.roundFormatPercent(percentOfHighBound);
        String parameters = String.format("%s_%s_%d", percentStr, FormatTool.format(targetRate), retry);
        intervalLength = config.getIntervalLength();
        String respHdrFile = String.format("%s/%s_response_time_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult resp = new HdrResult(operationName, "response_time", rateUnits, timeUnits, respHdrFile, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry);
        responseTimeWriter = new HdrLogWriterTask(resp, totalTime, writeHdr, config.getProgressIntervals());
        String servHdrFile = String.format("%s/%s_service_time_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult serv = new HdrResult(operationName, "service_time", rateUnits, timeUnits, servHdrFile, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry);
        serviceTimeWriter = new HdrLogWriterTask(serv, totalTime, writeHdr, config.getProgressIntervals());
        String errorsHdrFile = String.format("%s/%s_errors_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult erro = new HdrResult(operationName, "errors", rateUnits, timeUnits, errorsHdrFile, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry);
        errorsWriter = new HdrLogWriterTask(erro, totalTime, writeHdr, config.getProgressIntervals());
        if (config.isRawData()) {
            String rawName = String.format("%s_samples_data_%s.raw", operationName, parameters);
            rawDataOutputStream = new BufferedOutputStream(new FileOutputStream(new File(config.getHistogramsDir(), rawName)), 128 * 1024 * 1024);
            startTime0 = System.currentTimeMillis();
            rawDataOutputStream.write(String.format("# abs start time: %d ms since ZERO%n", startTime0).getBytes());
            startTime0 *= NS_IN_MS;
            rawDataOutputStream.write(String.format("startTime(us),intendedStartTime(us),finishTime(us),count,finishTime-startTime(us),finishTime-intendedStartTime(us),threadName%n").getBytes());
        }
    }

    void recordTimes(long startTime, long intendedStartTime, long finishTime, long count, boolean success) {
        if (success) {
            if (startTime > 0) {
                serviceTimeWriter.recordTime(finishTime - startTime, count);
            }
            if (intendedStartTime > 0) {
                responseTimeWriter.recordTime(finishTime - intendedStartTime, count);
            }
        } else {
            errorsWriter.recordTime(finishTime - startTime, count);
        }
        OutputStream rawStream = this.rawDataOutputStream;
        if (rawStream != null) {
            try {
                rawStream.write(String.format("%d,%d,%d,%d,%d,%d,%s%n"
                        , startTime > 0 ? (startTime - startTime0) / NS_IN_US : -1
                        , intendedStartTime > 0 ? (intendedStartTime - startTime0) / NS_IN_US : -1
                        , (finishTime - startTime0) / NS_IN_US
                        , count
                        , startTime > 0 && finishTime > startTime ? (finishTime - startTime) / NS_IN_US : -1
                        , intendedStartTime > 0 && finishTime > intendedStartTime ? (finishTime - intendedStartTime) / NS_IN_US : -1
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
        timer.scheduleAtFixedRate(responseTimeWriter, intervalLength, intervalLength);
        timer.scheduleAtFixedRate(serviceTimeWriter, intervalLength, intervalLength);
        timer.scheduleAtFixedRate(errorsWriter, intervalLength, intervalLength);
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

    void getResults(List<HdrResult> results) {
        if (results == null) {
            return;
        }
        if (!responseTimeWriter.isEmpty()) {
            results.add(responseTimeWriter.getHdrResult());
        }
        if (!serviceTimeWriter.isEmpty()) {
            results.add(serviceTimeWriter.getHdrResult());
        }
        if (!errorsWriter.isEmpty()) {
            results.add(errorsWriter.getHdrResult());
        }
    }
}

public class ResultsRecorder implements TimeRecorder {

    private Map<String, OperationsRecorder> operationsMap = new HashMap<>();
    private Timer timer = new Timer();
    private BenchmarkConfig config;
    private volatile boolean recordingStarted;
    private boolean writeHdr;
    private double percentOfHighBound;
    private double targetRate;
    private int totalTime;
    private int retry;

    private void startRecording() {
        if (!recordingStarted) {
            synchronized (this) {
                if (!recordingStarted) {
                    recordingStarted = true;
                    long startTime = System.currentTimeMillis();
                    operationsMap.forEach((s, r) -> r.startRecording(timer, startTime));
                }
            }
        }
    }

    @Override
    public void startRecording(String operationName, String rateUnits, String timeUnits) {
        try {
            operationsMap.put(operationName, new OperationsRecorder(operationName, rateUnits, timeUnits, config, percentOfHighBound, targetRate, retry, totalTime, writeHdr));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stopRecording() {
        cancel();
    }

    @Override
    public void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, long count, boolean success) {
        startRecording();
        operationsMap.get(operation).recordTimes(startTime, intendedStartTime, finishTime, count, success);
    }

    public ResultsRecorder(BenchmarkConfig config, double percentOfHighBound, double targetRate, int retry, int totalTime, boolean writeHdr) {
        this.config = config;
        this.percentOfHighBound = percentOfHighBound;
        this.targetRate = targetRate;
        this.retry = retry;
        this.totalTime = totalTime;
        this.writeHdr = writeHdr;
        HdrLogWriterTask.progressHeaderPrinted(false);
    }

    public void cancel() {
        timer.cancel();
        operationsMap.forEach((s,r) -> r.cancel());
    }

    public void getResults(List<HdrResult> results) {
        operationsMap.forEach((s,r) -> r.getResults(results));
    }

    public Collection<HdrResult> getResults() {
        ArrayList<HdrResult> results = new ArrayList<>();
        getResults(results);
        return results;
    }
}

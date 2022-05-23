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
import org.tussleframework.TussleRuntimeException;
import org.tussleframework.runners.RunnerConfig;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

import static org.tussleframework.tools.FormatTool.NS_IN_US;
import static org.tussleframework.tools.FormatTool.NS_IN_MS;

public class ResultsRecorder implements TimeRecorder {

class OperationsRecorder {

    private HdrLogWriterTask responseTimeWriter;
    private HdrLogWriterTask serviceTimeWriter;
    private HdrLogWriterTask errorsWriter;
    private OutputStream rawDataOutputStream;
    private boolean serviceTimeOnly;
    private long startTime0;
    private int intervalLength;

    public OperationsRecorder(String operationName, String rateUnits, String timeUnits) throws IOException {
        serviceTimeOnly = config.serviceTimeOnly;
        String percentStr = FormatTool.roundFormatPercent(runArgs.ratePercent);
        String parameters = String.format("%s_%s_%d", percentStr, FormatTool.format(runArgs.targetRate), runArgs.step);
        intervalLength = config.getIntervalLength();
        String respHdrFile = String.format("%s/%s_response_time_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult resp = new HdrResult(operationName, "response_time", rateUnits, timeUnits, respHdrFile, runArgs, 0, config);
        responseTimeWriter = new HdrLogWriterTask(resp, runArgs.runTime, writeHdr, config.getProgressIntervals());
        String servHdrFile = String.format("%s/%s_service_time_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult serv = new HdrResult(operationName, "service_time", rateUnits, timeUnits, servHdrFile, runArgs, 0, config);
        serviceTimeWriter = new HdrLogWriterTask(serv, runArgs.runTime, writeHdr, config.getProgressIntervals());
        String errorsHdrFile = String.format("%s/%s_errors_%s.hlog", config.getHistogramsDir(), operationName, parameters);
        HdrResult erro = new HdrResult(operationName, "errors", rateUnits, timeUnits, errorsHdrFile, runArgs, 0, config);
        errorsWriter = new HdrLogWriterTask(erro, runArgs.runTime, writeHdr, config.getProgressIntervals());
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
            if (intendedStartTime > 0 && !serviceTimeOnly) {
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

    void getResults(Collection<HdrResult> results) {
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
    private Map<String, OperationsRecorder> recordingsMap = new HashMap<>();
    private Set<String> recordingsFilter = new HashSet<>();
    private Timer timer = new Timer();
    private RunnerConfig config;
    private RunArgs runArgs;
    private boolean writeHdr;
    private boolean cancelOnStop;

    @Override
    public void startRecording(String operationName, String rateUnits, String timeUnits) {
        if (!recordingsFilter.isEmpty() && !recordingsFilter.contains(operationName)) {
            return;
        }
        if (recordingsMap.containsKey(operationName)) {
            /// throw new TussleRuntimeException("Operation already being recorded: " + operationName) ///
            return;
        }
        try {
            OperationsRecorder opRecorder = new OperationsRecorder(operationName, rateUnits, timeUnits);
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

    public ResultsRecorder(RunnerConfig config, RunArgs runArgs, boolean writeHdr, boolean cancelOnStop) {
        this.config = config;
        this.runArgs = runArgs;
        this.writeHdr = writeHdr;
        this.cancelOnStop = cancelOnStop;
        if (config.collectOps != null) {
            Collections.addAll(recordingsFilter, config.collectOps);
        }
        HdrLogWriterTask.progressHeaderPrinted(false);
    }

    public void cancel() {
        timer.cancel();
        recordingsMap.forEach((s,r) -> r.cancel());
    }

    public Collection<HdrResult> getResults(Collection<HdrResult> results) {
        recordingsMap.forEach((s,r) -> r.getResults(results));
        return results;
    }

    public Collection<HdrResult> getResults() {
        return getResults(new ArrayList<>());
    }
}

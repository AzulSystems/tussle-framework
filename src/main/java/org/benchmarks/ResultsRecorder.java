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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Timer;

import org.benchmarks.tools.FormatTool;
import org.benchmarks.tools.HdrResult;
import org.benchmarks.tools.LoggerTool;

public class ResultsRecorder implements TimeRecorder {

    private static final long NS_IN_MS = 1000000L;
    private static final long NS_IN_US = 1000L;

    public final HdrLogWriterTask responseTimeWriter;
    public final HdrLogWriterTask serviceTimeWriter;
    public final HdrLogWriterTask errorsWriter;

    private volatile boolean recordingStarted;
    private long startTime0 = 8;
    private int intervalLength;
    private OutputStream rawDataOutputStream;
    private Timer timer = new Timer();

    @Override
    public void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, boolean success) {
        startRecording();
        if (success) {
            if (startTime > 0) {
                serviceTimeWriter.recordTime(finishTime - startTime);
            }
            if (intendedStartTime > 0) {
                responseTimeWriter.recordTime(finishTime - intendedStartTime);
            }
        } else {
            errorsWriter.recordTime(finishTime - startTime);
        }
        if (rawDataOutputStream != null) {
            try {
                rawDataOutputStream.write(String.format("%s,%d,%d,%d,%d,%d,%d,%s%n", operation
                        , startTime > 0 ? (startTime - startTime0) / NS_IN_US : -1
                        , intendedStartTime > 0 ? (intendedStartTime - startTime0) / NS_IN_US : -1
                        , (finishTime - startTime0) / NS_IN_US
                        , success ? 1 : 0
                        , startTime > 0 && finishTime > startTime ? (finishTime - startTime) / NS_IN_US : -1
                        , intendedStartTime > 0 && finishTime > intendedStartTime ? (finishTime - intendedStartTime) / NS_IN_US : -1
                        , Thread.currentThread().getName()).getBytes());
            } catch (IOException e) {
                LoggerTool.logException(null, e);
                try {
                    rawDataOutputStream.close();
                } catch (IOException e2) {
                    ///
                }
                rawDataOutputStream = null;
            }
        }
    }

    private void startRecording() {
        if (!recordingStarted) {
            synchronized (this) {
                if (!recordingStarted) {
                    recordingStarted = true;
                    long startTime = System.currentTimeMillis();
                    responseTimeWriter.recordingStarted(startTime);
                    serviceTimeWriter.recordingStarted(startTime);
                    errorsWriter.recordingStarted(startTime);
                    timer.scheduleAtFixedRate(responseTimeWriter, intervalLength, intervalLength);
                    timer.scheduleAtFixedRate(serviceTimeWriter, intervalLength, intervalLength);
                    timer.scheduleAtFixedRate(errorsWriter, intervalLength, intervalLength);
                }
            }
        }
    }

    public ResultsRecorder(BenchmarkConfig config, double highBound, int percentOfHighBound, double targetRate, int retry, int totalTime) throws IOException {
        String operationName = String.format("op_%d_%s_%d", percentOfHighBound, FormatTool.format(targetRate), retry);
        intervalLength = config.getIntervalLength();
        String respHdrFile = String.format("%s/response_time_%d_%s_%d.hlog", config.getHistogramsDir(), percentOfHighBound, FormatTool.format(targetRate), retry);
        HdrResult resp = new HdrResult(operationName, "response_time", respHdrFile, highBound, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry, null);
        responseTimeWriter = new HdrLogWriterTask(resp, totalTime, config.getProgressIntervals());
        String servHdrFile = String.format("%s/service_time_%d_%s_%d.hlog", config.getHistogramsDir(), percentOfHighBound, FormatTool.format(targetRate), retry);
        HdrResult serv = new HdrResult(operationName, "service_time", servHdrFile, highBound, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry, null);
        serviceTimeWriter = new HdrLogWriterTask(serv, totalTime, config.getProgressIntervals());
        String errorsHdrFile = String.format("%s/errors_%d_%s_%d.hlog", config.getHistogramsDir(), percentOfHighBound, FormatTool.format(targetRate), retry);
        HdrResult erro = new HdrResult(operationName, "errors", errorsHdrFile, highBound, targetRate, 0, config.getHistogramFactor(), percentOfHighBound, intervalLength, 0, retry, null);
        errorsWriter = new HdrLogWriterTask(erro, totalTime, config.getProgressIntervals());
        if (config.isRawData()) {
            String rawName = String.format("samples_data_%d_%s_%d.raw", percentOfHighBound, FormatTool.format(targetRate), retry);
            rawDataOutputStream = new BufferedOutputStream(new FileOutputStream(new File(config.getHistogramsDir(), rawName)), 128 * 1024 * 1024);
            startTime0 = System.currentTimeMillis();
            rawDataOutputStream.write(String.format("# abs start time: %d ms since ZERO%n", startTime0).getBytes());
            startTime0 *= NS_IN_MS;
            rawDataOutputStream.write(String.format("operation,startTime(us),intendedStartTime(us),finishTime(us),success,finishTime-startTime(us),finishTime-intendedStartTime(us),threadName%n").getBytes());
        }
        HdrLogWriterTask.progressHeaderPrinted(false);
    }

    public void cancel() {
        timer.cancel();
        if (rawDataOutputStream != null) {
            try {
                rawDataOutputStream.flush();
            } catch (IOException e) {
                ///
            }
            try {
                rawDataOutputStream.close();
            } catch (IOException e) {
                ///
            }
        }
        responseTimeWriter.cancel();
        serviceTimeWriter.cancel();
        errorsWriter.cancel();
    }

    public void getResults(List<HdrResult> results) {
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

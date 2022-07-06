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

import static org.tussleframework.tools.FormatTool.roundFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.Recorder;
import org.tussleframework.HdrConfig;
import org.tussleframework.RunArgs;

public class HdrWriter extends TimerTask {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HdrWriter.class.getName());
    private static boolean progressHeaderPrinted;

    public static void progressHeaderPrinted(boolean b) {
        progressHeaderPrinted = b;
    }

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", HdrWriter.class.getSimpleName(), String.format(format, args)));
        }
    }

    private Path hdrFile;
    private Recorder recorder;
    private HdrResult hdrResult;
    private Histogram intervalHistogram;
    private Histogram progressHistogram;
    private HistogramLogWriter writer;
    private AtomicInteger countWrites = new AtomicInteger();
    private String shortName;
    private int totalTime;
    private int progressDelay;
    private int progressCount;
    private volatile long startTime;

    public HdrWriter(MetricInfo metricInfo, boolean writeHdr, int progressInterval, RunArgs runArgs, HdrConfig config, String histogramsDir) throws IOException {
        String respHdrFile = String.format("%s/%s", histogramsDir, metricInfo.formatFileName(runArgs));
        this.hdrResult = new HdrResult(metricInfo, respHdrFile, runArgs, config);
        this.recorder = new Recorder(Long.MAX_VALUE, 3);
        this.progressHistogram = new Histogram(3);
        this.hdrFile = Paths.get(hdrResult.hdrFile);
        this.totalTime = runArgs.runTime;
        this.progressDelay = progressInterval / 1000;
        shortName = " " + (hdrResult.metricName().length() > 4 ? hdrResult.metricName().substring(0, 4) : hdrResult.metricName());
        int remaining = 14 - shortName.length();
        shortName = (hdrResult.operationName().length() > remaining ? hdrResult.operationName().substring(0, remaining) : hdrResult.operationName()) + shortName;
        if (writeHdr) {
            Files.createDirectories(this.hdrFile.getParent());
            this.writer = new HistogramLogWriter(this.hdrFile.toFile());
        }
        /// log("Starting %s - %s", shortName, hdrResult.metricName)
    }

    public void recordingStarted(long startTime) {
        this.startTime = startTime;
    }

    public void recordTime(long value, long count) {
        if (value > 0) {
            if (count == 1) {
                recorder.recordValue(value);
            } else {
                recorder.recordValueWithCount(value, count);
            }
        }
    }

    public HdrResult getHdrResult() {
        return hdrResult;
    }

    public boolean isEmpty() {
        return countWrites.get() == 0;
    }

    @Override
    public synchronized void run() {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        if (intervalHistogram.getTotalCount() != 0) {
            hdrResult.add(intervalHistogram);
            if (progressDelay > 0) {
                progressHistogram.add(intervalHistogram);
            }
            HistogramLogWriter w = this.writer;
            if (w != null) {
                w.outputIntervalHistogram(intervalHistogram);
                countWrites.incrementAndGet();
            }
        }
        if (startTime == 0) {
            return;
        }
        progressCount++;
        if (progressCount < progressDelay) {
            return;
        }
        if (progressHistogram.getTotalCount() == 0) {
            return;
        }
        printProgress();
        progressCount = 0;
        progressHistogram.reset();
    }

    private void printProgress() {
        printProgressHeader();
        long spentTime = System.currentTimeMillis() - startTime;
        double progress = spentTime / 10.0 / totalTime;
        if (progress > 100) {
            progress = 100;
        }
        long time = spentTime / 1000;
        long totalCount = hdrResult.getCount();
        long count = progressHistogram.getTotalCount();
        double p50 = progressHistogram.getValueAtPercentile(50.0) / hdrResult.hdrFactor();
        double p90 = progressHistogram.getValueAtPercentile(90.0) / hdrResult.hdrFactor();
        double p99 = progressHistogram.getValueAtPercentile(99.0) / hdrResult.hdrFactor();
        double p100 = progressHistogram.getValueAtPercentile(100.0) / hdrResult.hdrFactor();
        double mean = progressHistogram.getMean() / hdrResult.hdrFactor();
        log("%14s | %6d | %5s%% | %8s | %8s | %8s | %8s | %8s | %8d | %8d", shortName, time, String.format("%2.1f", progress), roundFormat(p50), roundFormat(p90), roundFormat(p99), roundFormat(p100), roundFormat(mean), count, totalCount);
    }

    private void printProgressHeader() {
        if (!progressHeaderPrinted) {
            synchronized (logger) {
                if (!progressHeaderPrinted) {
                    progressHeaderPrinted = true;
                    log("---------------------------------------------------------------------------------------------------------------");
                    log("%14s | %6s | %6s | %8s | %8s | %8s | %8s | %8s | %8s | %8s", "name", "time", "progr", "p50ms", "p90ms", "p99ms", "p100ms", "mean", "count", "total");
                    log("---------------------------------------------------------------------------------------------------------------");
                }
            }
        }
    }

    @Override
    public boolean cancel() {
        boolean result = super.cancel();
        HistogramLogWriter w = this.writer;
        this.writer = null;
        if (w != null) {
            w.close();
            if (countWrites.get() == 0) {
                try {
                    Files.delete(hdrFile);
                } catch (IOException e) {
                    log("Failed to delete empty hdr file %s", hdrFile);
                }
            }
        }
        return result;
    }

    public int getCountWrites() {
        return countWrites.get();
    }
}

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

package org.tussleframework;

import static org.tussleframework.tools.FormatTool.*;

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

public class HdrLogWriterTask extends TimerTask {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HdrLogWriterTask.class.getName());
    private static boolean progressHeaderPrinted;

    public static void progressHeaderPrinted(boolean b) {
        progressHeaderPrinted = b;
    }

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", HdrLogWriterTask.class.getSimpleName(), String.format(format, args)));
        }
    }

    private Path hdrFile;
    private Recorder recorder;
    private HdrResult hdrResult;
    private Histogram allHistogram;
    private Histogram intervalHistogram;
    private Histogram progressHistogram;
    private HistogramLogWriter writer;
    private String shortName;
    private AtomicInteger countWrites = new AtomicInteger();
    private int totalTime;
    private int progressIntervals;
    private int progressCount;
    private volatile long startTime;

    public HdrLogWriterTask(HdrResult hdrResult, int totalTime, int progressIntervals) throws IOException {
        this.hdrResult = hdrResult;
        this.recorder = new Recorder(Long.MAX_VALUE, 3);
        this.allHistogram = new Histogram(3);
        this.progressHistogram = new Histogram(3);
        this.hdrFile = Paths.get(hdrResult.hdrFile);
        this.totalTime = totalTime;
        this.progressIntervals = progressIntervals;
        this.shortName = hdrResult.metricName.length() > 4 ? hdrResult.metricName.substring(0, 4) : hdrResult.metricName;
        Files.createDirectories(this.hdrFile.getParent());
        this.writer = new HistogramLogWriter(this.hdrFile.toFile());
        /// log("Starting %s - %s", shortName, hdrResult.metricName)
    }

    public void recordingStarted(long startTime) {
        this.startTime = startTime;
    }

    public void recordTime(long value) {
        if (value > 0) {
            recorder.recordValue(value);
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
            allHistogram.add(intervalHistogram);
            progressHistogram.add(intervalHistogram);
            writer.outputIntervalHistogram(intervalHistogram);
            countWrites.incrementAndGet();
        }
        if (startTime == 0) {
            return;
        }
        progressCount++;
        if (progressCount < progressIntervals) {
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
        long totalCount = allHistogram.getTotalCount();
        long count = progressHistogram.getTotalCount();
        double p50 = progressHistogram.getValueAtPercentile(50.0) / hdrResult.histogramFactor;
        double p90 = progressHistogram.getValueAtPercentile(90.0) / hdrResult.histogramFactor;
        double p99 = progressHistogram.getValueAtPercentile(99.0) / hdrResult.histogramFactor;
        double p100 = progressHistogram.getValueAtPercentile(100.0) / hdrResult.histogramFactor;
        double mean = progressHistogram.getMean() / hdrResult.histogramFactor;
        log("%6d | %4s | %5s%% | %7s | %7s | %7s | %7s | %7s | %7d | %7d", time, shortName, String.format("%2.1f", progress), roundFormat(p50), roundFormat(p90), roundFormat(p99), roundFormat(p100), roundFormat(mean), count, totalCount);
    }

    private void printProgressHeader() {
        if (!progressHeaderPrinted) {
            synchronized (logger) {
                if (!progressHeaderPrinted) {
                    progressHeaderPrinted = true;
                    log("----------------------------------------------------------------------------------------------");
                    log("%6s | %4s | %6s | %7s | %7s | %7s | %7s | %7s | %7s | %7s", "time", "name", "progr", "p50ms", "p90ms", "p99ms", "p100ms", "mean", "count", "total");
                    log("----------------------------------------------------------------------------------------------");
                }
            }
        }
    }

    @Override
    public boolean cancel() {
        boolean result = super.cancel();
        progressCount = progressIntervals;
        run();
        /// log("Closing %s", shortName)
        writer.close();
        if (countWrites.get() == 0) {
            try {
                Files.delete(hdrFile);
                log("Deleted empty hdr file without records %s", hdrFile);
            } catch (IOException e) {
                log("Failed to delete empty hdr file %s", hdrFile);
            }
        }
        return result;
    }

    public int getCountWrites() {
        return countWrites.get();
    }

    public Histogram getAllHistogram() {
        return allHistogram;
    }
}

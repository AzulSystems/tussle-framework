package org.benchmarks;

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
import org.benchmarks.tools.HdrResult;

import static org.benchmarks.tools.FormatTool.*;

public class HdrLogWriterTask extends TimerTask {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HdrLogWriterTask.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", HdrLogWriterTask.class.getSimpleName(), String.format(format, args)));
        }
    }

    private Recorder recorder = new Recorder(Long.MAX_VALUE, 3);
    private HdrResult hdrResult;
    private Histogram intervalHistogram;
    private Histogram allHistogram;
    private Histogram progressHistogram;
    private HistogramLogWriter writer;
    private Path hdrFile;
    private String shortName;
    private AtomicInteger countWrites = new AtomicInteger();
    private int totalTime;
    private int progressIntervals;
    private int progressCount;
    private volatile long startTime;
    private static boolean headerPrinted = false;

    public HdrLogWriterTask(HdrResult hdrResult, String hdrDir, int totalTime, int progressIntervals) throws IOException {
        this.hdrResult = hdrResult;
        this.allHistogram = new Histogram(3);
        this.progressHistogram = new Histogram(3);
        this.hdrFile = Paths.get(hdrDir, hdrResult.hdrFile);
        this.totalTime = totalTime;
        this.progressIntervals = progressIntervals;
        this.shortName = hdrResult.metricName.length() > 4 ? hdrResult.metricName.substring(0, 4) : hdrResult.metricName;
        Files.createDirectories(this.hdrFile.getParent());
        this.writer = new HistogramLogWriter(this.hdrFile.toFile());
        log("Start (%4s - %s)", shortName, hdrResult.metricName);
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
        long count = progressHistogram.getTotalCount();
        if (count == 0) {
            return;
        }
        progressCount = 0;
        long spentTime = System.currentTimeMillis() - startTime;
        double progress = spentTime / 10.0 / totalTime;
        if (progress > 100) {
            progress = 100;
        }
        long time = spentTime / 1000;
        long totalCount = allHistogram.getTotalCount();
        double p50 = progressHistogram.getValueAtPercentile(50.0) / hdrResult.histogramFactor;
        double p90 = progressHistogram.getValueAtPercentile(90.0) / hdrResult.histogramFactor;
        double p99 = progressHistogram.getValueAtPercentile(99.0) / hdrResult.histogramFactor;
        double p100 = progressHistogram.getValueAtPercentile(100.0) / hdrResult.histogramFactor;
        double mean = progressHistogram.getMean() / hdrResult.histogramFactor;
        synchronized (logger) {
            if (!headerPrinted) {
                log("--------------------------------------------------------------------------------------------");
                log("%6s | %4s | %6s | %7s | %7s | %7s | %7s | %7s | %6s | %6s", "time", "name", "progr", "p50ms", "p90ms", "p99ms", "p100ms", "mean", "count", "total");
                log("--------------------------------------------------------------------------------------------");
                headerPrinted = true;
            }
        }
        log("%6d | %4s | %5s%% | %7s | %7s | %7s | %7s | %7s | %6d | %6d", time, shortName, String.format("%2.1f", progress), roundFormat(p50), roundFormat(p90), roundFormat(p99), roundFormat(p100), roundFormat(mean), count, totalCount);
        progressHistogram.reset();
    }

    @Override
    public boolean cancel() {
        boolean result = super.cancel();
        progressCount = progressIntervals;
        run();
        log("Close (%4s)", shortName);
        writer.close();
        if (countWrites.get() == 0) {
            try {
                Files.delete(hdrFile);
                log("Empty file deleted %s", hdrFile);
            } catch (IOException e) {
                log("Failed to delete empty file %s", hdrFile);
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

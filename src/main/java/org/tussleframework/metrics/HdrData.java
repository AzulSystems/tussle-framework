package org.tussleframework.metrics;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.DoubleStream;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.tussleframework.HdrConfig;
import org.tussleframework.RunArgs;
import org.tussleframework.TussleException;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class HdrData {
    protected SortedMap<Long, Histogram> times = new TreeMap<>();
    protected SortedMap<Long, Histogram> latency = new TreeMap<>();
    protected Interval matchInterval;
    protected Interval interval = new Interval();
    protected MovingWindowSLE[] sleConfig;
    protected MetricInfo metricInfo;
    protected HdrConfig config;
    protected RunArgs runArgs;

    public HdrData(MetricInfo metricInfo, RunArgs runArgs, HdrConfig config, MovingWindowSLE[] sleConfig) {
        this.metricInfo = metricInfo;
        this.runArgs = runArgs;
        this.config = config;
        this.sleConfig = sleConfig;
        this.matchInterval = new Interval(config.hdrCutTime, Long.MAX_VALUE, "", false).scale(1000L);
    }

    public void recordValues(long stamp, long serviceTime, long latencyValue) {
        matchInterval.adjust(stamp);
        if (!matchInterval.contains(stamp, stamp)) {
            return;
        }
        interval.update(stamp);
        long intervalStamp = stamp / config.hdrInterval * config.hdrInterval;
        if (serviceTime >= 0) {
            Histogram h = times.computeIfAbsent(intervalStamp, key -> new Histogram(3));
            h.recordValue(serviceTime);
            if (h.getStartTimeStamp() > stamp) {
                h.setStartTimeStamp(stamp);
            }
            if (h.getEndTimeStamp() <= stamp) {
                h.setEndTimeStamp(stamp + 1);
            }
        }
        if (latencyValue >= 0) {
            Histogram h = latency.computeIfAbsent(intervalStamp, key -> new Histogram(3));
            h.recordValue(latencyValue);
            if (h.getStartTimeStamp() > stamp) {
                h.setStartTimeStamp(stamp);
            }
            if (h.getEndTimeStamp() <= stamp) {
                h.setEndTimeStamp(stamp + 1);
            }
        }
    }

    public void getMetrics(MetricData metricData, double[] percentiles) {
        if (times.size() > 0) {
            metricData.add(makeMetric(getNameTimes(), times, percentiles));
        }
        if (latency.size() > 0) {
            metricData.add(makeMetric(getNameLatency(), latency, percentiles));
        }
    }

    public String getNameTimes() {
        String name = metricInfo.metricName;
        if (name == null || name.isEmpty()) {
            name = HdrResult.SERVICE_TIME;
        }
        return name;
    }

    public String getNameLatency() {
        String name = metricInfo.metricName;
        if (name == null || name.isEmpty()) {
            name = HdrResult.RESPONSE_TIME;
        } else if (times.size() > 0 && latency.size() > 0) {
            name = name + "_" +HdrResult.RESPONSE_TIME;
        }
        return name;
    }

    class TimesIter {
        long intervalStampStart;
        long intervalStampFinish;
        long intervalStamp;
        long totalCount;
        SortedMap<Long, Histogram> ts;
        TimesIter(SortedMap<Long, Histogram> ts) {
            this.ts = ts;
            intervalStampStart = interval.start / config.hdrInterval * config.hdrInterval;
            intervalStampFinish = interval.finish / config.hdrInterval * config.hdrInterval;
            for (intervalStamp = intervalStampStart; intervalStamp <= intervalStampFinish; intervalStamp += config.hdrInterval) {
                if (ts.containsKey(intervalStamp)) {
                    break;
                }
            }
        }
        AbstractHistogram nextIntervalHistogram() {
            Histogram histogram = null;
            if (ts.containsKey(intervalStamp)) {
                histogram = ts.get(intervalStamp);
                totalCount += histogram.getTotalCount();
            }
            intervalStamp += config.hdrInterval;
            return histogram;
        }
    }

    public void saveHdrs() throws TussleException {
        saveHdr(getNameTimes(), times);
        saveHdr(getNameLatency(), latency);
    }

    public Collection<HdrResult> getHdrResults(Collection<HdrResult> results) {
        getHdrResults(results, getNameTimes(), times);
        getHdrResults(results, getNameLatency(), latency);
        return results;
    }

    public Collection<HdrResult> getHdrResults() {
        return getHdrResults(new ArrayList<>());
    }

    public void getHdrResults(Collection<HdrResult> results, String name, SortedMap<Long, Histogram> ts) {
        if (ts.size() > 0) {
            String hdrFile = new File(config.histogramsDir, metricInfo.replaceMetricName(name).formatFileName(runArgs, "hlog")).getAbsolutePath();
            HdrResult hdrResult = new HdrResult(metricInfo.replaceMetricName(name), hdrFile, runArgs, config);
            hdrResult.loadHdrData(new TimesIter(ts)::nextIntervalHistogram, sleConfig, new Interval[] {
                    new Interval(interval.start / 1000, interval.finish / 1000 + 1, interval.name, true)
            });
            LoggerTool.log(getClass().getSimpleName(), "HdrResults: %s - %f %f %f", hdrResult.getOpName(), hdrResult.getRate(), hdrResult.getMean(), hdrResult.getMaxValue());
            results.add(hdrResult);
        }
    }

    protected void saveHdr(String name, SortedMap<Long, Histogram> ts) throws TussleException {
        String hdrFile = new File(config.histogramsDir, metricInfo.replaceMetricName(name).formatFileName(runArgs, "hlog")).getAbsolutePath();
        long intervalStampStart = interval.start / config.hdrInterval * config.hdrInterval;
        long intervalStampFinish = interval.finish / config.hdrInterval * config.hdrInterval;
        try (PrintStream os = new PrintStream(hdrFile)) {
            HistogramLogWriter writer = new HistogramLogWriter(os);
            for (long intervalStamp = intervalStampStart; intervalStamp <= intervalStampFinish; intervalStamp += config.hdrInterval) {
                if (ts.containsKey(intervalStamp)) {
                    Histogram intervalHistogram = ts.get(intervalStamp);
                    writer.outputIntervalHistogram(intervalHistogram);
                }
            }
        } catch (Exception e) {
            throw new TussleException(e);
        }
    }

    protected Metric makeMetric(String name, SortedMap<Long, Histogram> ts, double[] percentiles) {
        DoubleStream.Builder[] buffValues = new DoubleStream.Builder[percentiles.length + 1];
        for (int i = 0; i < buffValues.length; i++) {
            buffValues[i] = DoubleStream.builder();
        }
        long totalCount = 0;
        long intervalStampStart = interval.start / config.hdrInterval * config.hdrInterval;
        long intervalStampFinish = interval.finish / config.hdrInterval * config.hdrInterval;
        LoggerTool.log(getClass().getSimpleName(), "makeMetric %s %d, interval %d, start %d, finish %d (%d)", name, ts.size(), config.hdrInterval, intervalStampStart, intervalStampFinish, (interval.finish - interval.start));
        for (long intervalStamp = intervalStampStart; intervalStamp <= intervalStampFinish; intervalStamp += config.hdrInterval) {
            if (ts.containsKey(intervalStamp)) {
                Histogram histogram = ts.get(intervalStamp);
                totalCount += histogram.getTotalCount();
                for (int i = 0; i < percentiles.length; i++) {
                    buffValues[i].add(histogram.getValueAtPercentile(percentiles[i]) / config.hdrFactor);
                }
                buffValues[percentiles.length].add(histogram.getTotalCount());
            } else {
                for (int i = 0; i < percentiles.length; i++) {
                    buffValues[i].add(0);
                }
                buffValues[percentiles.length].add(0);
            }
        }
        Metric metric = Metric.builder()
                .name(name)
                .operation(metricInfo.operationName)
                .host(metricInfo.hostName)
                .start(interval.start)
                .finish(interval.finish)
                .delay(config.hdrInterval)
                .totalValues(totalCount)
                .actualRate(interval.finish > interval.start ? totalCount / ((interval.finish - interval.start) / 1000.0) : 0)
                .build();
        for (int i = 0; i < percentiles.length; i++) {
            metric.add(new MetricValue("P" + FormatTool.roundFormat(percentiles[i]).replace(".", "_") + "_VALUES", buffValues[i].build().toArray()));
        }
        metric.add(new MetricValue(MetricType.COUNTS, buffValues[percentiles.length].build().toArray()));
        return metric;
    }
}

package org.benchmarks.tools;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.benchmarks.metrics.Interval;
import org.benchmarks.metrics.Marker;
import org.benchmarks.metrics.Metric;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.MetricType;
import org.benchmarks.metrics.MetricValue;
import org.benchmarks.metrics.MovingWindowSumHistogram;
import org.benchmarks.metrics.SLA;

public class HdrIntervalResult {

    private static final MetricType[] valueTypes = {
            MetricType.COUNTS,
            MetricType.P0_VALUES,
            MetricType.P50_VALUES,
            MetricType.P90_VALUES,
            MetricType.P99_VALUES,
            MetricType.P999_VALUES,
            MetricType.P9999_VALUES,
            MetricType.P100_VALUES,
    };

    public static final int valueTypeCount() {
        return valueTypes.length;
    }

    public static final MetricType valueType(int index) {
        return valueTypes[index];
    }

    private SLA[] slaConfig;
    private Metric metric;
    private Interval interval;
    private Histogram histogram;
    private double histogramFactor;
    private double[] movingWindowMax;
    private DoubleStream.Builder[] valBuffers;
    private DoubleStream.Builder[] mwBuffValues;
    private DoubleStream.Builder[] mwBuffCounts;
    private MovingWindowSumHistogram[] movingWindowSumHistograms;

    static long mul(long v, long m) {
        if (v == Long.MAX_VALUE || v == Long.MIN_VALUE)
            return v;
        else
            return v * m;
    }

    public HdrIntervalResult(SLA[] slaConfig, Interval interval, double histogramFactor) {
        this.slaConfig = slaConfig;
        this.histogram = new Histogram(3);
        this.histogramFactor = histogramFactor;
        this.interval = new Interval(mul(interval.start, 1000), mul(interval.finish , 1000), interval.name);
        valBuffers = new DoubleStream.Builder[valueTypes.length];
        for (int i = 0; i < valBuffers.length; i++) {
            valBuffers[i] = DoubleStream.builder();
        }
        mwBuffValues = new DoubleStream.Builder[slaConfig.length];
        mwBuffCounts = new DoubleStream.Builder[slaConfig.length];
        for (int i = 0; i < slaConfig.length; i++) {
            mwBuffValues[i] = DoubleStream.builder();
            mwBuffCounts[i] = DoubleStream.builder();
        }
        movingWindowSumHistograms = new MovingWindowSumHistogram[slaConfig.length];
        movingWindowMax = new double[slaConfig.length];
        for (int i = 0; i < slaConfig.length; i++) {
            double percentile = slaConfig[i].percentile;
            int movingWindow = slaConfig[i].movingWindow;
            movingWindowSumHistograms[i] = new MovingWindowSumHistogram(new Histogram(3), new LinkedList<>(), percentile, movingWindow);
        }
    }

    public Metric getMetric() {
        return metric;
    }

    public void adjustInterval(long stamp) {
        interval.adjust(stamp);
    }

    public void add(List<AbstractHistogram> histos) {
        Histogram histoSum = new Histogram(3);
        int added = 0;
        for (AbstractHistogram h : histos) {
            long stamp1 = h.getStartTimeStamp();
            long stamp2 = h.getEndTimeStamp();
            if (interval.contains(stamp1, stamp2)) {
                histogram.add(h);
                histoSum.add(h);
                added++;
                for (int i = 0; i < movingWindowSumHistograms.length; i++) {
                    MovingWindowSumHistogram mwsh = movingWindowSumHistograms[i];
                    mwsh.add(h);
                    double mwValue = mwsh.sumHistogram.getValueAtPercentile(mwsh.percentile) / histogramFactor;
                    long mwCount = mwsh.sumHistogram.getTotalCount();
                    mwBuffValues[i].add(mwValue);
                    mwBuffCounts[i].add(mwCount);
                    if (movingWindowMax[i] < mwValue) {
                        movingWindowMax[i] = mwValue;
                    }
                }
            }
        }
        if (added > 0) {
            for (int i = 0; i < valueTypes.length; i++) {
                valBuffers[i].add(valueTypes[i].getValue(histoSum, histogramFactor));
            }
        }
    }

    public void processHistogram(HdrResult hdrResult, MetricData metricData, double[] percentiles, int mergeHistos) {
        String opName = hdrResult.getOperationName();
        String metricIntervalName = (hdrResult.metricName + " " + interval.name).trim();
        long totalCount = histogram.getTotalCount();
        long start = histogram.getStartTimeStamp();
        long finish = histogram.getEndTimeStamp();
        LoggerTool.log("HdrIntervalResult", "processHistogram interval [%s] totalCount %d, start %d, finish %d", interval, totalCount, start, finish);
        if (totalCount == 0 || finish <= start) {
            return;
        }
        double actualRate = histogram.getTotalCount() / ((finish - start) / 1000.0);
        double meanValue = histogram.getMean() / histogramFactor;
        metric = Metric.builder()
                .name(metricIntervalName)
                .units("ms")
                .operation(opName)
                .start(start)
                .finish(finish)
                .delay(hdrResult.intervalLength * mergeHistos)
                .totalValues(totalCount)
                .targetRate(hdrResult.targetRate)
                .actualRate(actualRate)
                .meanValue(meanValue)
                .build();
        if (hdrResult.highBound > 0) {
            metric.setHighBound(hdrResult.highBound);
        }
        metricData.add(metric);
        for (int i = 0; i < valueTypes.length; i++) {
            MetricValue mValue = new MetricValue(valueTypes[i].name(), valBuffers[i].build().toArray());
            metric.add(mValue);
        }
        DoubleStream.Builder buffPercentileValues = DoubleStream.builder();
        DoubleStream.Builder buffPercentileCounts = DoubleStream.builder();
        long highValue = histogram.getValueAtPercentile(100);
        if (percentiles != null && percentiles.length > 0) {
            for (double p : percentiles) {
                long pValue = histogram.getValueAtPercentile(p);
                double pval = pValue / histogramFactor;
                buffPercentileValues.add(pval);
                long pcount = histogram.getCountBetweenValues(pValue, highValue);
                buffPercentileCounts.add(pcount);
            }
            metric.add(new MetricValue("PERCENTILE_NAMES", percentiles));
            metric.add(new MetricValue("PERCENTILE_VALUES", buffPercentileValues.build().toArray()));
            metric.add(new MetricValue("PERCENTILE_COUNTS", buffPercentileCounts.build().toArray()));
        }
        for (int i = 0; i < slaConfig.length; i++) {
            LoggerTool.log("HdrIntervalResult", "slaConfig " + slaConfig[i].longName()  + " max " + movingWindowMax[i]);
            String mwMetricName = (hdrResult.metricName + " " + slaConfig[i].withMWName() + " " + interval.name).trim();
            Metric mwMetric = Metric.builder()
                    .name(mwMetricName)
                    .units("ms")
                    .operation(opName)
                    .start(start)
                    .finish(finish)
                    .delay(hdrResult.intervalLength)
                    .totalValues(totalCount)
                    .targetRate(hdrResult.targetRate)
                    //.type("mw")
                    //.group(valName)
                    .build();
            double[] values = mwBuffValues[i].build().toArray();
            double[] counts = mwBuffCounts[i].build().toArray();
            mwMetric.add(new MetricValue("VALUES", values));
            mwMetric.add(new MetricValue("COUNTS", counts));
            if (slaConfig[i].maxValue > 0) {
                mwMetric.addMarker(new Marker(slaConfig[i].withMaxName(), null, slaConfig[i].maxValue));
            }
            metricData.add(mwMetric);
            // TODO: targetMetric.add(new MetricValue(valName + "_max", movingWindowMax[i]))
        }
    }
}

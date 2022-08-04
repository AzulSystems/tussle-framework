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

import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.DoubleStream;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.tussleframework.HdrConfig;

public class HdrIntervalResult {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HdrIntervalResult.class.getName());

    private static final MetricType[] metricTypes = {
            MetricType.COUNTS,
            MetricType.P0_VALUES,
            MetricType.P50_VALUES,
            MetricType.P90_VALUES,
            MetricType.P99_VALUES,
            MetricType.P99_9_VALUES,
            MetricType.P99_99_VALUES,
            MetricType.P100_VALUES,
    };

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", HdrIntervalResult.class.getSimpleName(), String.format(format, args)));
        }
    }

    public static final int metricTypeCount() {
        return metricTypes.length;
    }

    public static final MetricType metricType(int index) {
        return metricTypes[index];
    }

    public int skippedHistos;
    private Metric metric;
    private HdrConfig config;
    private Interval interval;
    private Histogram histogram;
    private double[] movingWindowMaxValues;
    private MovingWindowSLE[] sleConfig;
    private DoubleStream.Builder[] metricValues;
    private DoubleStream.Builder[] movingWindowValues;
    private DoubleStream.Builder[] movingWindowCounts;
    private MovingWindowHistogram[] movingWindowHistograms;

    public HdrIntervalResult(Interval interval, HdrConfig config, MovingWindowSLE[] sleConfig) {
        this.config = config;
        this.histogram = new Histogram(3);
        this.interval = new Interval(interval);
        metricValues = new DoubleStream.Builder[metricTypes.length];
        for (int i = 0; i < metricValues.length; i++) {
            metricValues[i] = DoubleStream.builder();
        }
        sleConfig = sleConfig != null ? sleConfig : new MovingWindowSLE[0];
        movingWindowValues = new DoubleStream.Builder[sleConfig.length];
        movingWindowCounts = new DoubleStream.Builder[sleConfig.length];
        for (int i = 0; i < movingWindowValues.length; i++) {
            movingWindowValues[i] = DoubleStream.builder();
            movingWindowCounts[i] = DoubleStream.builder();
        }
        movingWindowHistograms = new MovingWindowHistogram[sleConfig.length];
        movingWindowMaxValues = new double[sleConfig.length];
        for (int i = 0; i < sleConfig.length; i++) {
            movingWindowHistograms[i] = new MovingWindowHistogram(sleConfig[i], config.hdrFactor);
        }
        this.sleConfig = sleConfig;
    }

    public Histogram getHistogram() {
        return histogram;
    }

    public double getRate() {
        long fullTime = getTimeMs();
        return fullTime > 0 ? histogram.getTotalCount() / (fullTime / 1000.0) : 0;
    }

    public long getTimeMs() {
        return histogram.getEndTimeStamp() - histogram.getStartTimeStamp();
    }

    public long getCount() {
        return histogram.getTotalCount();
    }

    public double getMean() {
        return histogram.getMean() / config.hdrFactor;
    }

    public double getMaxValue() {
        return histogram.getMaxValue() / config.hdrFactor;
    }

    public double getValueAtPercentile(double percentile) {
        return histogram.getValueAtPercentile(percentile) / config.hdrFactor;
    }

    public Metric getMetric() {
        return metric;
    }

    public void adjustInterval(long stamp) {
        interval.adjust(stamp);
    }

    public void addHistogram(AbstractHistogram inputHistogram) {
        histogram.add(inputHistogram);
    }

    public void addHistograms(Collection<AbstractHistogram> inputHistograms) {
        Histogram inputHistogramsSum = new Histogram(3);
        int addedHistos = 0;
        for (AbstractHistogram inputHistogram : inputHistograms) {
            if (interval.contains(inputHistogram.getStartTimeStamp(), inputHistogram.getEndTimeStamp())) {
                addHistogram(inputHistogram);
                inputHistogramsSum.add(inputHistogram);
                addedHistos++;
                for (int i = 0; i < movingWindowHistograms.length; i++) {
                    MovingWindowHistogram mwh = movingWindowHistograms[i];
                    mwh.add(inputHistogram);
                    long mwCount = mwh.getCount();
                    double mwValue = mwh.getValue();
                    movingWindowCounts[i].add(mwCount);
                    movingWindowValues[i].add(mwValue);
                    if (movingWindowMaxValues[i] < mwValue) {
                        movingWindowMaxValues[i] = mwValue;
                    }
                }
            } else {
                skippedHistos++;
            }
        }
        if (addedHistos > 0) {
            for (int i = 0; i < metricTypes.length; i++) {
                metricValues[i].add(metricTypes[i].getValue(inputHistogramsSum, config.hdrFactor));
            }
        }
    }

    public void getMetrics(HdrResult hdrResult, MetricData metricData, double[] percentiles) {
        String metricIntervalName = (hdrResult.metricName() + " " + interval.name).trim();
        if (hdrResult.runArgs.name != null && !hdrResult.runArgs.name.isEmpty() && !hdrResult.runArgs.name.equals("run")) {
            metricIntervalName += " " + hdrResult.runArgs.name;
        }
        long start = histogram.getStartTimeStamp();
        long finish = histogram.getEndTimeStamp();
        long totalCount = histogram.getTotalCount();
        log("getMetrics %s [%s] totalCount %d, start %d, finish %d", metricIntervalName, interval, totalCount, start, finish);
        if (totalCount == 0 || finish <= start) {
            return;
        }
        metric = Metric.builder()
                .start(start)
                .finish(finish)
                .name(metricIntervalName)
                .units(hdrResult.timeUnits())
                .operation(hdrResult.operationName())
                .delay(config.reportInterval)
                .totalValues(totalCount)
                .retry(hdrResult.runArgs.runStep)
                .percentOfHighBound(hdrResult.runArgs.ratePercent)
                .targetRate(hdrResult.runArgs.targetRate)
                .rateUnits(hdrResult.rateUnits())
                .actualRate(getRate())
                .meanValue(histogram.getMean() / config.hdrFactor)
                .build();
        metricData.add(metric);
        for (int i = 0; i < metricTypes.length; i++) {
            metric.add(new MetricValue(metricTypes[i].name(), metricValues[i].build().toArray()));
        }
        DoubleStream.Builder buffPercentileValues = DoubleStream.builder();
        DoubleStream.Builder buffPercentileCounts = DoubleStream.builder();
        long highValue = histogram.getValueAtPercentile(100);
        if (percentiles != null && percentiles.length > 0) {
            for (double p : percentiles) {
                long pValue = histogram.getValueAtPercentile(p);
                buffPercentileValues.add(pValue / config.hdrFactor);
                buffPercentileCounts.add(histogram.getCountBetweenValues(pValue, highValue));
            }
            metric.add(new MetricValue(MetricType.PERCENTILE_NAMES, percentiles));
            metric.add(new MetricValue(MetricType.PERCENTILE_VALUES, buffPercentileValues.build().toArray()));
            metric.add(new MetricValue(MetricType.PERCENTILE_COUNTS, buffPercentileCounts.build().toArray()));
        }
        getMovingWindowSLEMetrics(hdrResult, metricData);
    }

    protected void getMovingWindowSLEMetrics(HdrResult hdrResult, MetricData metricData) {
        long start = histogram.getStartTimeStamp();
        long finish = histogram.getEndTimeStamp();
        log("getMetrics sleConfig count %d", sleConfig.length);
        for (int i = 0; i < sleConfig.length; i++) {
            double[] values = movingWindowValues[i].build().toArray();
            double[] counts = movingWindowCounts[i].build().toArray();
            log("  sleConfig %s, max %f, values %d, counts %d", sleConfig[i].longName(), movingWindowMaxValues[i], values.length, counts.length);
            String mwMetricName = (hdrResult.metricName() + " " + sleConfig[i].longName() + " " + interval.name).trim();
            Metric mwMetric = Metric.builder()
                    .name(mwMetricName)
                    .units(hdrResult.timeUnits())
                    .operation(hdrResult.operationName())
                    .start(start)
                    .finish(finish)
                    .delay(config.reportInterval)
                    .retry(hdrResult.runArgs.runStep)
                    .percentOfHighBound(hdrResult.runArgs.ratePercent)
                    .targetRate(hdrResult.runArgs.targetRate)
                    //.type("mw")
                    //.group(valName)
                    .build();
            mwMetric.add(new MetricValue(MetricType.VALUES, values));
            mwMetric.add(new MetricValue(MetricType.COUNTS, counts));
            if (sleConfig[i].markerValue() > 0) {
                mwMetric.addMarker(new Marker(sleConfig[i].markerName(), null, sleConfig[i].markerValue()));
            }
            metricData.add(mwMetric);
            // TODO: targetMetric.add(new MetricValue(valName + "_max", movingWindowMax[i]))
        }
    }
}

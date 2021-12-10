package org.benchmarks.tools;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogReader;
import org.benchmarks.metrics.Interval;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.SLA;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class HdrResult {

    public String operationName;
    public String metricName;
    public String hdrFile;
    public double highBound;
    public double targetRate;
    public double actualRate;
    public double histogramFactor;
    public int percentOfHighBound;
    public int intervalLength;
    public int recordsCount;
    public int retry;
    public List<HdrIntervalResult> subIntervalHistograms;

    public void detectValues(String fileName) {
        String[] parts = fileName.split("_");
        int nums = 0;
        try {
            retry = Integer.valueOf(parts[parts.length - 1]);
            targetRate = Double.valueOf(parts[parts.length - 2]);
            percentOfHighBound = Integer.valueOf(parts[parts.length - 3]);
            nums = 3;
        } catch (NumberFormatException e) {
            try {
                retry = Integer.valueOf(parts[parts.length - 1]);
                targetRate = 0;
                percentOfHighBound = Integer.valueOf(parts[parts.length - 2]);
                nums = 2;
            } catch (NumberFormatException e2) {
                retry = 0;
                targetRate = 0;
                percentOfHighBound = 0;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - nums; i++) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(parts[i]);
        }
        metricName = sb.toString();
        operationName = nums > 0 ? String.format("op_%d_%s_%d", percentOfHighBound, FormatTool.format(targetRate), retry) : "";
    }

    public static String clearPathAndExtension(String fileName) {
        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            fileName = fileName.substring(pos + 1);
        }
        pos = fileName.lastIndexOf('.');
        if (pos >= 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    public static HdrResult getIterationResult(String fileName) {
        HdrResult result = new HdrResult();
        result.hdrFile = fileName;
        result.intervalLength = 1000;
        final String hs = ".hdr-";
        if (fileName.startsWith("tlp_stress_metrics") && fileName.indexOf(hs) >= 0) {
            result.histogramFactor = 1000;
            result.metricName = "service_time";
            result.operationName = fileName.substring(fileName.indexOf(hs) + hs.length());
        } else {
            result.histogramFactor = 1000_000;
            result.detectValues(clearPathAndExtension(fileName));
        }
        return result;
    }

    public void processHistograms(MetricData metricData, InputStream inputStream, SLA[] slaConfig, Interval[] intervals, double[] percentiles, int mergeHistos) {
        LoggerTool.log("HdrResult", "processHistogram '%s', operation %s, metricName %s, merge %d adjucent histograms", hdrFile, getOperationName(), metricName, mergeHistos);
        recordsCount = 0;
        try (HistogramLogReader hdrReader = new HistogramLogReader(inputStream)) {
            int nulls = 0;
            double rangeStartTimeSec = 0.0;
            double rangeEndTimeSec = Double.MAX_VALUE;
            subIntervalHistograms = new ArrayList<>();
            for (Interval interval : intervals) {
                subIntervalHistograms.add(new HdrIntervalResult(slaConfig, interval, histogramFactor));
            }
            Histogram firstHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            if (firstHistogram == null) {
                return;
            }
            final long stampStart = firstHistogram.getStartTimeStamp();
            subIntervalHistograms.forEach(sh -> sh.adjustInterval(stampStart));
            while (true) {
                ArrayList<AbstractHistogram> histos = new ArrayList<>();
                int hi = 0;
                if (firstHistogram != null) {
                    hi = 1;
                    histos.add(firstHistogram);
                    firstHistogram = null;
                }
                while (hi < mergeHistos) {
                    AbstractHistogram intervalHistogram = (AbstractHistogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
                    if (intervalHistogram != null) {
                        histos.add(intervalHistogram);
                    }
                    hi++;
                }
                if (!histos.isEmpty()) {
                    recordsCount++;
                    subIntervalHistograms.forEach(sh -> sh.add(histos));
                } else if (nulls++ > 10) {
                    break;
                }
            }
            subIntervalHistograms.forEach(sh -> sh.processHistogram(this, metricData, percentiles, mergeHistos));
        }
    }

    public boolean checkSLA(SLA sla) {
        try (HistogramLogReader hdrReader = new HistogramLogReader(hdrFile)) {
            double rangeStartTimeSec = 0.0;
            double rangeEndTimeSec = Double.MAX_VALUE;
            Queue<Histogram> movingWindowQueue = new LinkedList<>();
            Histogram intervalHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            Histogram movingWindowSumHistogram = new Histogram(3);
            while (intervalHistogram != null) {
                long windowCutOffTimeStamp = intervalHistogram.getEndTimeStamp() - sla.movingWindow * 1000;
                movingWindowSumHistogram.add(intervalHistogram);
                Histogram head = movingWindowQueue.peek();
                while (head != null && head.getEndTimeStamp() <= windowCutOffTimeStamp) {
                    Histogram prevHist = movingWindowQueue.remove();
                    movingWindowSumHistogram.subtract(prevHist);
                    head = movingWindowQueue.peek();
                }
                movingWindowQueue.add(intervalHistogram);
                if (movingWindowSumHistogram.getValueAtPercentile(sla.percentile) / histogramFactor > sla.maxValue) {
                    return false;
                }
                intervalHistogram = (Histogram) hdrReader.nextIntervalHistogram(rangeStartTimeSec, rangeEndTimeSec);
            }
        } catch (Exception e) {
            LoggerTool.logException(null, e);
            return true;
        }
        return true;
    }
}

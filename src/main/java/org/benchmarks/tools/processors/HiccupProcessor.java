package org.benchmarks.tools.processors;

import java.io.InputStream;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.HistogramLogReader;
import org.benchmarks.metrics.Metric;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.MetricValue;

public class HiccupProcessor implements DataLogProcessor {
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        DoubleStream.Builder valBuffers = DoubleStream.builder();
        try (HistogramLogReader hdrReader = new HistogramLogReader(inputStream)) {
            int nulls = 0;
            while (true) {
                AbstractHistogram interval = (AbstractHistogram) hdrReader.nextIntervalHistogram(0, Double.MAX_VALUE);
                if (interval != null) {
                    if (start == 0) {
                        start = interval.getStartTimeStamp();
                    }
                    valBuffers.add(interval.getMaxValue() / 1000000.0);
                } else if (nulls++ > 10) {
                    break;
                }
            }
        }
        metricData.add(Metric.builder()
                .name("hiccup_times")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(5000)
                .build()
                .add(new MetricValue("VALUES", valBuffers.build().toArray())));
       return true;
   }
}

package org.benchmarks.tools.processors;

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.benchmarks.metrics.Metric;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.MetricValue;
import org.benchmarks.tools.FormatTool;
import org.benchmarks.tools.LoggerTool;

public class IpstatProcessor implements DataLogProcessor {
    /**
    *
DELAY: 5
START: 2021-09-08 06:33:11,511
HOST: hostname

RX bytes, RX packets, TX bytes, TX packets
4756, 54, 9044, 63
198, 3, 618, 3
86, 1, 144, 2
1554, 11, 1260, 16
86, 1, 144, 2
74, 1, 54, 1
1044198, 1528, 1899322, 1822
133116713, 136194, 129911695, 143722
124521594, 142658, 124896307, 151017
...
    */
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        int intervalLengthS = 5;
        DoubleStream.Builder buffRxValues = DoubleStream.builder();
        DoubleStream.Builder buffTxValues = DoubleStream.builder();
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("DELAY:")) {
                    intervalLengthS = Integer.valueOf(line.substring("DELAY:".length()).trim());
                } else if (line.startsWith("START:")) {
                    start = FormatTool.parseUTCDate(line);
                } else if (line.startsWith("HOST:")) {
                    host = line.substring("HOST:".length()).trim();
                } else if (line.startsWith("RX bytes, RX packets, TX bytes, TX packets")) {
                    // skip
                } else if (!line.isEmpty()) {
                    String[] s = line.split(",");
                    if (s.length == 4) {
                        buffRxValues.add(Double.valueOf(s[0].trim()) / 1024 / 1024 / intervalLengthS);
                        buffTxValues.add(Double.valueOf(s[2].trim()) / 1024 / 1024 / intervalLengthS);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        String units = "MiB/s";
        metricData.add(Metric.builder()
                .name("network")
                .operation("tx")
                .units(units)
                .host(host)
                .start(start)
                .finish(finish)
                .delay(intervalLengthS * 1000)
                .build()
                .add(new MetricValue("VALUES", buffTxValues.build().toArray())));
        metricData.add(Metric.builder()
                .name("network")
                .operation("rx")
                .units(units)
                .host(host)
                .start(start)
                .finish(finish)
                .delay(intervalLengthS * 1000)
                .build()
                .add(new MetricValue("VALUES", buffRxValues.build().toArray())));
        return true;
    }
}

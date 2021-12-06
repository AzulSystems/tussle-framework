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

public class DiskstatProcessor implements DataFileProcessor {

    /**
     * Supported format:
DELAY: 5
START: 2021-09-08 06:33:11,510
HOST: hostname

Linux 5.4.0-1045-aws (ip-172-31-94-166) <------>09/08/21 <----->_x86_64_<------>(8 CPU)

06:33:11          DEV       tps     rkB/s     wkB/s   areq-sz    aqu-sz     await     svctm     %util
06:33:16        loop0      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16        loop1      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16        loop2      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16      nvme1n1      0.60      0.00      4.80      8.00      0.00      0.00      4.00      0.24
06:33:16      nvme0n1      0.20      0.80      0.00      4.00      0.00      1.00      4.00      0.08
     */
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        int intervalLength = 5000;
        DoubleStream.Builder buffValues = DoubleStream.builder();
        try (Scanner scanner = new Scanner(inputStream)) {
            boolean accum = false;
            double accumValue = 0; 
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("DELAY:")) {
                    intervalLength = Integer.valueOf(line.substring("DELAY:".length()).trim()) * 1000;
                } else if (line.startsWith("START:")) {
                    finish = start = FormatTool.parseUTCDate(line);
                } else if (line.startsWith("HOST:")) {
                    host = line.substring("HOST:".length()).trim(); 
                } else if (accum) {
                    line = line.trim();
                    if (line.length() == 0) {
                        accum = false;
                        buffValues.add(accumValue);
                        accumValue = 0;
                    } else {
                        String[] s = line.split("\\s+");
                        if (s.length == 10 && s[0].length() == 8) {
                            accumValue += Double.valueOf(s[9].trim().replace(',', '.'));
                            finish += intervalLength;
                        } else {
                            break;
                        }
                    }
                } else if (line.indexOf("DEV") > 0) {
                    accum = true;
                }
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        metricData.add(Metric.builder()
                .name("disk")
                .units("%util")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(intervalLength)
                .build()
                .add(new MetricValue("VALUES", buffValues.build().toArray())));
        return true;
    }
}

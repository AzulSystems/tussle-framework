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

public class TopProcessor implements DataLogProcessor {

    /**
     * Supported format: 
DELAY: 5
START: 2021-09-08 06:33:10,491
HOST: hostname
 
top - 06:34:40 up  5:14,  0 users,  load average: 2.67, 0.76, 0.32
Tasks: 149 total,   1 running,  77 sleeping,   0 stopped,   0 zombie
%Cpu(s): 49.8 us,  9.1 sy,  0.0 ni, 37.0 id,  0.2 wa,  0.0 hi,  3.9 si,  0.0 st
KiB Mem : 65049720 total, 17650368 free,   864144 used, 46535208 buff/cache
KiB Swap:        0 total,        0 free,        0 used. 21471036 avail Mem

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
24812 ubuntu    20   0 24.952t 0.313t 0.313t S 500.6  517   2:37.86 /localhome/ubuntu/product/bin/java -Xms40g -Xmx40g -XX:+PrintGCDetails ...
23936 root      20   0       0      0      0 I   1.2  0.0   0:00.14 [kworker/u16:2-f]
   10 root      20   0       0      0      0 S   0.2  0.0   0:01.86 [ksoftirqd/0]
   11 root      20   0       0      0      0 I   0.2  0.0   0:03.20 [rcu_sched]
24648 ubuntu    20   0   44532   4128   3524 R   0.2  0.0   0:00.11 top -i -c -b -d 5 -w 512
24664 ubuntu    20   0   13316   3324   3012 S   0.2  0.0   0:00.04 bash /localhome/ubuntu/kafka/tools/ipstats.sh

top - 06:34:45 up  5:15,  0 users,  load average: 3.02, 0.86, 0.36
...
     */
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        int intervalLength = 5000;
        String cpuP1 = "%Cpu(s):";
        String cpuP2 = "Cpu(s):";
        DoubleStream.Builder buffValues = DoubleStream.builder();
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("DELAY:")) {
                    intervalLength = Integer.valueOf(line.substring("DELAY:".length()).trim()) * 1000;
                } else if (line.startsWith("START:")) {
                    start = FormatTool.parseUTCDate(line);
                } else if (line.startsWith("HOST:")) {
                    host = line.substring("HOST:".length()).trim(); 
                } else if (line.startsWith(cpuP1) || line.startsWith(cpuP2)) {
                    if (line.startsWith(cpuP1)) {
                        line = line.substring(cpuP1.length());
                        line = line.substring(0, line.indexOf(" us"));
                    } else {
                        line = line.substring(cpuP2.length());
                        line = line.substring(0, line.indexOf("%us"));
                    }
                    line = line.trim().replace(',', '.');
                    buffValues.add(Double.parseDouble(line));
                }
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        metricData.add(Metric.builder()
                .name("top")
                .units("%cpu")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(intervalLength)
                .build()
                .add(new MetricValue("VALUES", buffValues.build().toArray())));
        return true;
    }

}

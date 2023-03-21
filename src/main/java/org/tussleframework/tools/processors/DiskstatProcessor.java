/*
 * Copyright (c) 2021-2023, Azul Systems
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

package org.tussleframework.tools.processors;

import static org.tussleframework.tools.FormatTool.matchFilters;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.tussleframework.BasicProperties;
import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricType;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class DiskstatProcessor implements DataLogProcessor {

    private static Double d(String s) {
        return Double.valueOf(s.trim().replace(',', '.'));
    }

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
...

or

08:17:47 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
08:17:52 AM   nvme0n1     12.80     44.80    251.00     23.11      0.02      1.39      0.16      0.20

08:17:52 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
08:17:57 AM   nvme0n1      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00

08:17:57 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
...
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean processData(MetricData metricData, HdrData hdrData, BasicProperties processorsProps, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        int intervalLength = 5000;
        Map<String, DoubleStream.Builder> buffers = new HashMap<>();
        Map<String, Integer> indices = new HashMap<>();
        List<String> devInclude = null;
        List<String> devExclude = null;
        Set<String> cols = null;
        if (processorsProps != null && processorsProps.getProps("diskstat") != null) {
            Map<String, Object> props = processorsProps.getProps("diskstat");
            devInclude = (List<String>) props.get("devInclude"); 
            devExclude = (List<String>) props.get("devExclude");
            if (props.get("cols") != null) {
                cols = new HashSet<>((List<String>) props.get("cols"));
                if (cols.contains("util")) {
                    cols.remove("util");
                    cols.add("%util");
                }
            }
        }
        if (cols == null) {
            cols = new HashSet<>(Arrays.asList("%util"));
        }
        cols.forEach(col -> buffers.put(col, DoubleStream.builder()));
        try (Scanner scanner = new Scanner(inputStream)) {
            boolean accum = false;
            Map<String, Double> accums = new HashMap<>();
            cols.forEach(col -> accums.put(col, 0.0));
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
                    if (line.length() == 0 || line.indexOf(" Terminated") >= 1) {
                        accum = false;
                        buffers.forEach((col, buffer) -> buffer.add(accums.get(col)));
                        cols.forEach(col -> accums.put(col, 0.0));
                        finish += intervalLength;
                    } else {
                        String[] s = line.split("\\s+");
                        if ((s.length == 10 || s.length == 11) && s[0].length() == 8) {
                            String dev = s[indices.get("DEV")];
                            if (matchFilters(dev, devInclude, devExclude)) {
                                cols.forEach(col -> accums.put(col, accums.get(col) + d(s[indices.get(col)])));
                            }
                        } else {
                            break;
                        }
                    }
                } else if (line.indexOf(" DEV ") > 8) {
                    if (indices.isEmpty()) {
                        String[] header = line.split("\\s+");
                        if ((header.length == 10 || header.length == 11) && header[0].length() == 8) {
                            for (int i = header.length - 9; i < header.length; i++) {
                                indices.put(header[i], i);
                            }
                        } else {
                            break;
                        }
                    }
                    accum = true;
                }
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        String hostf = host;
        long startf = start;
        long finishf = finish;
        int intervalLengthf = intervalLength;
        cols.forEach(col -> metricData.add(Metric.builder()
                .name("disk")
                .operation(col)
                .units(col)
                .host(hostf)
                .start(startf)
                .finish(finishf)
                .delay(intervalLengthf)
                .build()
                .add(new MetricValue(MetricType.VALUES, buffers.get(col).build().toArray()))
                ));
        return true;
    }
}

/*
 * Copyright (c) 2021, Azul Systems
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

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

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
    public boolean processData(MetricData metricData, HdrData hdrData, InputStream inputStream, String host, Logger logger) {
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

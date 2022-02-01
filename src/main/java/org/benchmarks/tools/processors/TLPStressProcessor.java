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

package org.benchmarks.tools.processors;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.benchmarks.metrics.Metric;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.MetricValue;
import org.benchmarks.tools.LoggerTool;

public class TLPStressProcessor implements DataLogProcessor {
    /**
    *
# tlp-stress run at Tue Nov 09 20:01:33 UTC 2021
# run BasicTimeSeries --duration 75m --partitions 100M --threads 8 --populate 200000 --readrate 0.2 --rate 20k --partitiongenerator sequence --concurrency 50 --port 9042 --host 10.22.4.157 --csv ./tlp_stress_metrics_1.csv --hdr ./t
lp_stress_metrics_1.hdr
,,Mutations,,,Reads,,,Deletes,,,Errors,
Timestamp, Elapsed Time,Count,Latency (min),Latency (p50),Latency (p90),Latency (p99),Latency (p99.9),Latency (p99.99),Latency (max),1min (req/s),Count,Latency (min),Latency (p50),Latency (p90),Latency (p99),Latency (p99.9),Latency (p99.99),Latency (max),1min (req/s),Count,Latency (min),Latency (p50),Latency (p90),Latency (p99),Latency (p99.9),Latency (p99.99),Latency (max),1min (req/s),Count,1min (errors/s)
2021-11-09T20:02:58.463608846Z,85,63430,0.221789,0.588731,2.484343,8.117892,16.130761,21.843908,21.843908,973.5602725294648,16008,0.29789699999999997,0.6910919999999999,3.573883,16.663673,34.837198,39.883835999999995,39.883835999999995,246.16725623923946,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:01.462734642Z,88,111157,0.240725,0.5285949999999999,1.145897,6.630927,12.810882999999999,16.130761,16.130761,973.5602725294648,27929,0.274064,0.620308,1.629422,13.237670999999999,20.730507,39.883835999999995,39.883835999999995,246.16725623923946,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:04.462701341Z,91,159233,0.249522,0.5020049999999999,0.8207,6.019037,12.542691999999999,16.130761,16.130761,2175.759639479047,39852,0.274064,0.592446,1.027177,11.130009,20.691074999999998,39.883835999999995,39.883835999999995,545.6195326160566,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:07.462690658Z,94,207186,0.21951099999999998,0.487824,0.7167359999999999,4.3490269999999995,7.031826,9.614575,9.614575,2175.759639479047,51880,0.255735,0.56713,0.8387789999999999,9.915884,16.303819999999998,16.663673,16.663673,545.6195326160566,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:10.462678752Z,97,255078,0.199577,0.462364,0.669933,3.5191079999999997,6.630927,7.031826,7.031826,3280.4132428923213,63983,0.255735,0.5469729999999999,0.779208,9.48658,15.357505,16.303819999999998,16.303819999999998,822.7440297690714,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:13.462675772Z,100,303046,0.199577,0.44537,0.6511739999999999,3.48977,6.630927,7.031826,7.031826,4297.031460920193,75990,0.26955999999999997,0.5283519999999999,0.7500749999999999,6.773524,15.357505,16.303819999999998,16.303819999999998,1077.2311420194385,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
2021-11-09T20:03:16.462675111Z,103,351315,0.212729,0.43723399999999996,0.6351249999999999,3.302708,6.630927,7.031826,7.031826,4297.031460920193,87722,0.26955999999999997,0.523818,0.7341,5.4710339999999995,14.875808999999999,15.357505,15.357505,1077.2311420194385,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0.0
...
    */
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        final String S1 = ",,Mutations,,,Reads,,,Deletes,,,Errors,";
        final String metricName = "service_time";
        long start = 0;
        long finish = 0;
        int[] idxW = {
                3, // min
                4, // p50
                5, // p90
                6, // p99
                7, // p99.9
                8, // p99.99
                9, // max
        };
        int[] idxR = {
                12, // min
                13, // p50
                14, // p90
                15, // p99
                16, // p99.9
                17, // p99.99
                18, // max
        };
        String[] valueTypes = {
                "P0_VALUES",    // min
                "P50_VALUES",   // p50
                "P90_VALUES",   // p90
                "P99_VALUES",   // p99
                "P999_VALUES",  // p99.9
                "P9999_VALUES", // p99.99
                "P100_VALUES",  // max
                "COUNTS",       // count
        };
        String[] errorValueTypes = {
                "VALUES",  // values
                "COUNTS",  // count
        };
        DoubleStream.Builder[] valBuffersW = new DoubleStream.Builder[valueTypes.length];
        for (int i = 0; i < valBuffersW.length; i++) {
            valBuffersW[i] = DoubleStream.builder();
        }
        DoubleStream.Builder[] valBuffersR = new DoubleStream.Builder[valueTypes.length];
        for (int i = 0; i < valBuffersR.length; i++) {
            valBuffersR[i] = DoubleStream.builder();
        }
        DoubleStream.Builder[] valBuffersE = new DoubleStream.Builder[errorValueTypes.length];
        for (int i = 0; i < valBuffersE.length; i++) {
            valBuffersE[i] = DoubleStream.builder();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS Z");
        try (Scanner s = new Scanner(inputStream)) {
            String[] h1 = null;
            String[] h2 = null;
            while (s.hasNext()) {
                String line = s.nextLine().trim();
                if (line.startsWith("#")) {
                    logger.log(Level.INFO, "Comment: {0}", line);
                } else if (!line.equals(S1)) {
                    logger.log(Level.INFO, "Unexpected header: {0}", line);
                    return false;
                } else {
                    h1 = line.split(",");
                    if (s.hasNext()) {
                        line = s.nextLine();
                        h2 = line.split(",");
                        if (h2.length != 31) {
                            logger.log(Level.INFO, "Unexpected line: {0}", line);
                            return false;
                        }
                    }
                    break;
                }
            }
            if (h1 == null || h2 == null) {
                logger.log(Level.INFO, "Unexpected header line: null");
                return false;
            }
            String line = null;
            long prevCountW = 0;
            long prevCountR = 0;
            long prevCountE = 0;
            while (s.hasNext()) {
                line = s.nextLine();
                String[] values = line.split(",");
                long stamp = dateFormat.parse(values[0].substring(0, 23).replace('T', ' ').replace('.', ':') + " UTC").getTime();
                if (start == 0) {
                    start = stamp;
                }
                finish = stamp + 3000;
                long totalCountW = Long.parseLong(values[2]);
                long countW = totalCountW - prevCountW;
                prevCountW = totalCountW;
                long totalCountR = Long.parseLong(values[11]);
                long countR = totalCountR - prevCountR;
                prevCountR = totalCountR;
                long totalCountE = Long.parseLong(values[29]);
                long countE = totalCountE - prevCountE;
                prevCountE = totalCountE;
                for (int i = 0; i < idxW.length; i++) {
                    valBuffersW[i].add(Double.valueOf(values[idxW[i]]));
                }
                valBuffersW[valBuffersW.length - 1].add(countW);
                for (int i = 0; i < idxR.length; i++) {
                    valBuffersR[i].add(Double.valueOf(values[idxR[i]]));
                }
                valBuffersR[valBuffersR.length - 1].add(countR);
                valBuffersE[0].add(Double.valueOf(values[30]));
                valBuffersE[1].add(countE);
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        Metric writesMetric = Metric.builder()
                .name(metricName)
                .operation("TLP-Writes")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(3000)
                .build();
        for (int i = 0; i < valBuffersW.length; i++) {
            writesMetric.add(new MetricValue(valueTypes[i], valBuffersW[i].build().toArray()));
        }
        Metric readsMetric = Metric.builder()
                .name(metricName)
                .operation("TLP-Reads")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(3000)
                .build();
        for (int i = 0; i < valBuffersR.length; i++) {
            readsMetric.add(new MetricValue(valueTypes[i], valBuffersR[i].build().toArray()));
        }
        Metric errorsMetric = Metric.builder()
                .name("1min (errors/s)")
                .operation("TLP-Errors")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(3000)
                .build();
        for (int i = 0; i < valBuffersE.length; i++) {
            errorsMetric.add(new MetricValue(errorValueTypes[i], valBuffersE[i].build().toArray()));
        }
        metricData.add(readsMetric);
        metricData.add(writesMetric);
        metricData.add(errorsMetric);
        return true;
   }
}


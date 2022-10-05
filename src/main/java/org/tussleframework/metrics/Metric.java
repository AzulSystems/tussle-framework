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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static org.tussleframework.tools.FormatTool.*;

import lombok.Builder;
import lombok.Data;

/**
 * Generic metric representation. It can be simple single value metric e.g. { name: 'score', value: ) 
 * or full set of arrays containing different percentiles and counts. 
 * 
 * @author rus
 *
 */
@Data
@Builder
public class Metric {
    protected Long start;        // absolute measurement UTC start time in ms
    protected Long finish;       // absolute measurement UTC finish time in ms
    protected Long totalValues;  // total measured values (samples, requests, etc.)
    protected Integer delay;     // metric measurement interval length
    protected Integer retry;
    protected String name;       // response_time, service_time, etc. 
    protected String host;       // host or node name where metric was collected
    protected String type;
    protected String group;
    protected String units;      // metric measurement value units (ms, s, etc.) 
    protected String xunits;     // alternative metric X-axis units if it is not time-based
    protected String rateUnits;  // metric measurement rate units (op/s, msg/s, etc.)
    protected String operation;  // reads, writes, appends, end-to-end, etc.
    protected Double highBound;  // optional target rate high-bound value which used for iterative scenarios, e.g. run at target rates from 50% to 110% of high-bound 
    protected Double targetRate; // target rate
    protected Double actualRate; // measured actual rate
    protected Double value;      // metric value in case if it is single-value
    protected Double meanValue;  // mean value of measured metric values
    protected Double percentOfHighBound;
    protected String[] xValues;  // alternative metric X-axis values or labels
    protected ArrayList<Marker> markers;
    protected ArrayList<MetricValue> metricValues; // measured metric values

    public Metric add(MetricValue mv) {
        if (metricValues == null) {
            metricValues = new ArrayList<>();
        }
        metricValues.add(mv);
        return this;
    }

    public Metric addMarker(Marker marker) {
        if (markers == null) {
            markers = new ArrayList<>();
        }
        markers.add(marker);
        return this;
    }

    public MetricValue byType(String type) {
        if (metricValues == null)
            return null;
        Optional<MetricValue> elem = metricValues.stream().filter(m -> type.equals(m.type)).findFirst();
        return elem.orElse(null);
    }

    public MetricValue byType(MetricType type) {
        return byType(type.name());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        scoreOn().forEach(s -> sb.append(String.format("%s%n", s)));
        return sb.toString();
    }

    public Collection<String> scoreOn() {
        ArrayList<String> res = new ArrayList<>();
        String opName = "";
        if (operation != null) {
            opName += operation + " ";
        }
        if (name != null) {
            opName += name + " ";
        }
        if (percentOfHighBound != null) {
            opName += roundFormatPercent(percentOfHighBound) + " ";
        }
        if (targetRate != null && targetRate > 0) {
            opName += roundFormat(targetRate) + " ";
        }
        if (retry != null && retry > 0) {
            opName += retry + " ";
        }
        opName = opName.trim().replace(" ", "_");
        if (value != null) {
            res.add(String.format("%s: %s %s", opName, roundFormat(value), units != null ? units : ""));
        }
        if (actualRate != null) {
            res.add(String.format("%s_actual_rate: %s %s", opName, roundFormat(actualRate), rateUnits != null ? rateUnits : ""));
        }
        MetricValue pnames = byType(MetricType.PERCENTILE_NAMES.name());
        MetricValue pvalues = byType(MetricType.PERCENTILE_VALUES.name());
        if (pnames != null && pvalues != null) {
            for (int i = 0; i < pnames.values.length; i++) {
                double p = pnames.values[i];
                if (p == 50 || p == 99 || p == 99.9) {
                    res.add(String.format("%s_p%s: %s %s", opName, format(p), roundFormat(pvalues.values[i]), units != null ? units : ""));
                }
            }
        }
        return res;
    }
}

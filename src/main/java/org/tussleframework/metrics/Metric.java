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

package org.tussleframework.metrics;

import java.util.ArrayList;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Metric {
    protected Long start;
    protected Long finish;
    protected Long totalValues;
    protected Integer delay;
    protected String name;
    protected String host;
    protected String type;
    protected String group;
    protected String units;
    protected String xunits;
    protected String operation;
    protected Double highBound;
    protected Double targetRate;
    protected Double actualRate;
    protected Double value;
    protected Double meanValue;
    protected String[] xValues;
    protected ArrayList<Marker> markers;
    protected ArrayList<MetricValue> metricValues;

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
}
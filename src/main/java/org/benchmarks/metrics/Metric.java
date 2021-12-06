package org.benchmarks.metrics;

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
package org.benchmarks.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Marker {
    protected String name;
    protected Double xValue;
    protected Double yValue;
}

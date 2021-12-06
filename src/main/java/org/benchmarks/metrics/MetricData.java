package org.benchmarks.metrics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.benchmarks.RunProperties;
import org.benchmarks.tools.Informator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MetricData {
    protected RunProperties runProperties;
    protected ArrayList<Metric> metrics = new ArrayList<>();

    public void add(Metric m) {
        metrics.add(m);
    }

    public void loadRunProperties(String file) throws IOException {
        ObjectReader or = new ObjectMapper().reader();
        runProperties = or.readValue(new File(file), RunProperties.class);
    }

    public void loadDefaultRunProperties() {
        runProperties = new RunProperties();
        runProperties.getHardware().putAll(Informator.getHwInfo());
        runProperties.getOs().putAll(Informator.getOsInfo());
        runProperties.getJvm().putAll(Informator.getJvmInfo());
    }
}

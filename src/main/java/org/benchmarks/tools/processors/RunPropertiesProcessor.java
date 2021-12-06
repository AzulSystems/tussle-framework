package org.benchmarks.tools.processors;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.benchmarks.RunProperties;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.tools.JsonTool;
import org.benchmarks.tools.LoggerTool;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
class RunPropertiesData {
    RunProperties runProperties = new RunProperties();
}

@Data
@ToString
class RunPropertiesDoc {
    RunPropertiesData doc = new RunPropertiesData();
}

public class RunPropertiesProcessor implements DataFileProcessor {
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        try {
            RunPropertiesDoc doc = JsonTool.readJson(inputStream, RunPropertiesDoc.class);
            if (logger.isLoggable(Level.INFO)) {
                logger.info(doc.toString());
            }
            metricData.setRunProperties(doc.doc.runProperties);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        return true;
    }
}

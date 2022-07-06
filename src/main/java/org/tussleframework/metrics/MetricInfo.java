package org.tussleframework.metrics;

import org.tussleframework.RunArgs;
import org.tussleframework.tools.FormatTool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricInfo {
    private static String responseTime = "response_time";
    private static String serviceTime = "service_time";
    private static String responseTime2 = "response-time";
    private static String serviceTime2 = "service-time";
    private static String intendedPref = "intended-";

    public String operationName;
    public String metricName;
    public String rateUnits;
    public String timeUnits;
    public String hostName;

    public MetricInfo replaceMetricName(String metricName) {
        return new MetricInfo(operationName, metricName, rateUnits, timeUnits, hostName);
    }

    public String formatFileName(RunArgs runArgs) {
        return String.format("%s_%s_%s_%s_%d.hlog", operationName, metricName,
                FormatTool.roundFormatPercent(runArgs.ratePercent), FormatTool.format(runArgs.targetRate), runArgs.runStep);
    }

    public void fillValues(String[] parts, int filledParts, String defaultName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - filledParts; i++) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(parts[i]);
        }
        String opName;
        String metrName = sb.toString();
        int pos = metrName.indexOf('_');
        if (pos > 0) {
            opName = metrName.substring(0, pos);
            metrName = metrName.substring(pos + 1);
        } else {
            opName = metrName;
            metrName = defaultName;
            if (opName.toLowerCase().startsWith(intendedPref) && (metrName.equals(serviceTime) || metrName.equals(serviceTime2))) {
                opName = opName.substring(intendedPref.length());
                metrName = responseTime;
            }
        }
        metrName = metrName.replace(serviceTime2, serviceTime).replace(responseTime2, responseTime);
        this.operationName = opName;
        this.metricName = metrName;
    }
}

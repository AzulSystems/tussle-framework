package org.benchmarks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunResult {
    public Exception runError;
    public String rateUnits;
    public String timeUnits;
    public double rate;
    public long errors;
    public long count;
    public long time;
}

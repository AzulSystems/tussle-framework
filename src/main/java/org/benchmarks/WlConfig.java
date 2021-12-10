package org.benchmarks;

import org.benchmarks.tools.FormatTool;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WlConfig extends BenchmarkConfig {
    public int threads = 1;
    public boolean asyncMode = false;

    @Override
    public void validate() {
        super.validate();
        if (FormatTool.parseValue(targetRate) == 0 && asyncMode) {
            throw new IllegalArgumentException(String.format("Invalid targetRate(%s) - should be positive for async mode", targetRate));
        }
        if (threads < 1) {
            throw new IllegalArgumentException(String.format("Invalid threads(%d) - should be non-negative", threads));
        }
    }
}

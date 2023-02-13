package org.tussleframework.tools.processors;

import org.tussleframework.HdrConfig;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SamplesProcessorConfig extends HdrConfig {
    public long timestampFactor = 1000; // multiplier to milliseconds
    public boolean hasHeader = true;
}

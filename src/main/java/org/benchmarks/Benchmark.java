package org.benchmarks;

/**
 * Basic Benchmark interface
 * 
 */
public interface Benchmark {

    /**
     * Benchmark initialisation, invoked by Benchmark Runner before all other operations  
     *  
     * @param args
     * @throws Exception
     */
    void init(String[] args) throws Exception;

    /**
     * Benchmark reset, can be invoked by Benchmark Runner before run operation.
     * 
     * @throws Exception
     */
    void reset() throws Exception;

    /**
     * Benchmark cleanup, invoked by Benchmark Runner after all other operations
     */
    void cleanup();

    /**
     * Get Benchmark name
     * 
     * @return Benchmark name
     */
    String getName();

    /**
     * Get Benchmark configuration
     * 
     * @return Benchmark configuration
     */
    BenchmarkConfig getConfig();

    /**
     * Runs Benchmark at the specified target rate for specified run time  
     * 
     * @param targetRate - target rate  
     * @param warmupTime
     * @param runTime
     * @param recorder
     * 
     * @return RunResult 
     */
    RunResult run(double targetRate, int warmupTime, int runTime, TimeRecorder recorder);
}

package org.benchmarks.tools;

import java.util.concurrent.locks.LockSupport;

public class SleepTool {

    private SleepTool() {
    }

    /**
     * Loop-spinning sleep function
     *
     * @param ns - sleep interval in nanoseconds
     */
    public static void sleepSpinning(long ns) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < ns) {
            /// nothing
        }
    }

    /**
     * @param deadline - System.nanoTime's
     */
    public static void sleepBySpinning(long deadline) {
        while (deadline - System.nanoTime() > 0) {
            /// nothing
        }
    }

    /**
     * General sleep function
     * 
     * @param ns - sleep interval in nanoseconds 
     */
    public static void sleep(long ns) {
        sleepUntil(System.nanoTime() + ns);
    }

    /**
     * @param deadline - System.nanoTime's
     */
    public static void sleepUntil(long deadline) {
        long ns = deadline - System.nanoTime();
        while (ns > 20_000L) {
            LockSupport.parkNanos(ns - 20_000);
            ns = deadline - System.nanoTime();
        }
        sleepSpinning(ns);
    }
}

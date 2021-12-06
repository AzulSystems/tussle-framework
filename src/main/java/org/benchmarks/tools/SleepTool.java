package org.benchmarks.tools;

import java.util.concurrent.locks.LockSupport;

public class SleepTool {

    private SleepTool() {
    }

    public static void sleepPrecise(long ns) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < ns) {
            /// nothing
        }
    }

    /**
     * @param deadline - System.nanoTime's
     */
    public static void sleepPreciseUntil(long deadline) {
        while (deadline - System.nanoTime() > 0) {
            /// nothing
        }
    }

    public static void sleep(long ns) {
        sleepUntil(System.nanoTime() + ns);
    }

    /**
     * @param deadline - System.nanoTime's
     */
    public static void sleepUntil(long deadline) {
        long ns = deadline - System.nanoTime();
        while (ns > 20_000L) {
            LockSupport.parkNanos(ns);
            ns = deadline - System.nanoTime();
        }
        sleepPrecise(ns);
    }
}

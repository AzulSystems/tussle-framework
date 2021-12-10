package org.benchmarks;

public interface WithException {
    void run() throws Exception;

    public static void wrapException(WithException r) {
        try {
            r.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void withException(WithException r) throws Exception {
        try {
            r.run();
        } catch (RuntimeException e) {
            throw (Exception) e.getCause();
        }
    }
}
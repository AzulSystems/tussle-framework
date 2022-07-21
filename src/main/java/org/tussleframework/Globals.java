package org.tussleframework;

public class Globals {
    private Globals(){}
    public static final long NS_IN_S = 1_000_000_000L;
    public static final long NS_IN_MS = 1_000_000L;
    public static final long NS_IN_US = 1_000L;
    public static final long MS_IN_S = 1_000L;
    public static final long NANO_TIME_OFFSET = System.currentTimeMillis() * NS_IN_MS - System.nanoTime();
}

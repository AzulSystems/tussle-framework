package org.benchmarks;

import java.util.HashMap;
import java.util.Properties;

public class RunProperties extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public synchronized void setProperty(String key, Object o) {
        put(key, o);
    }

    public synchronized String getProperty(String key) {
        Object o = get(key);
        return o != null ? o.toString() : null;
    }

    public synchronized Properties getPropMap(String key) {
        Properties[] prop = (Properties[]) get(key);
        if (prop == null) {
            prop = new Properties[1];
            prop[0] = new Properties();
            put(key, prop);
        }
        return prop[0];
    }

    public synchronized Properties getHardware() {
        return getPropMap("hardware");
    }

    public synchronized Properties getOs() {
        return getPropMap("os");
    }

    public synchronized Properties getJvm() {
        return getPropMap("jvm");
    }
}

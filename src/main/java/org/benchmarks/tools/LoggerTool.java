package org.benchmarks.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggerTool {

    private LoggerTool() {
    }

    public static void init(String name) {
        try {
            LogManager.getLogManager().readConfiguration(new SequenceInputStream(Analyzer.class.getResourceAsStream("/logging.properties"),
                    new ByteArrayInputStream(String.format("java.util.logging.FileHandler.pattern = %s.log", name).getBytes())));
        } catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "[LoggerTool] LogManager readConfiguration failed", e);
        }
    }

    public static void logException(Logger loggerIn, Exception e) {
        Logger logger = loggerIn == null ? Logger.getGlobal() : loggerIn;
        if (e.getMessage() == null) {
            if (e.getCause() != null) {
                logger.log(Level.SEVERE, e.getCause(), () -> "failed: " + e.getCause().getMessage());
            }
        } else {
            logger.log(Level.SEVERE, e, () -> "failed: " + e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        e.printStackTrace(new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                sb.append(cbuf, off, len);
            }
            @Override
            public void flush() throws IOException {
                //
            }
            @Override
            public void close() throws IOException {
                //
            }
        }));
        logger.log(Level.SEVERE, e, sb::toString);
    }

    public static void log(String name, String format, Object... args) {
        Logger logger = Logger.getGlobal();
        if (logger.isLoggable(Level.INFO)) {
            if (name != null) {
                logger.info(String.format("[%s] %s", name, String.format(format, args)));
            } else {
                logger.info(String.format(format, args));
            }
        }
    }
}

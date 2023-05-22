/*
 * Copyright (c) 2021-2023, Azul Systems
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of [project] nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package org.tussleframework.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.tussleframework.runners.BasicRunner;

public class LoggerTool {

    private LoggerTool() {
    }

    public static OutputStream nullOutputStream() {
        return new OutputStream() {
            private volatile boolean closed;

            private void ensureOpen() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public void write(int b) throws IOException {
                ensureOpen();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ensureOpen();
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    public static void init(String name, String handlers, String ...extraProps) {
        try {
            Vector<InputStream> ins = new Vector<>();
            ins.add(LoggerTool.class.getResourceAsStream("/logging.properties"));
            if (handlers != null) {
                ins.add(new ByteArrayInputStream(String.format("java.util.logging.FileHandler.pattern = %s.log %nhandlers = %s%n", name, handlers).getBytes()));
            } else {
                ins.add(new ByteArrayInputStream(String.format("java.util.logging.FileHandler.pattern = %s.log%n", name).getBytes()));
            }
            if (extraProps != null) {
                for (String prop : extraProps) {
                    ins.add(new ByteArrayInputStream(String.format("%s%n", prop).getBytes()));
                }
            }
            LogManager.getLogManager().readConfiguration(new SequenceInputStream(ins.elements()));
        } catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "[LoggerTool] LogManager readConfiguration failed", e);
        }
    }

    public static void init(String name) {
        init(name, null);
    }

    public static void logException(Exception e) {
        logException(Logger.getGlobal(), e);
    }

    public static void log(Logger loggerIn, String format, Object... args) {
        Logger logger = loggerIn == null ? Logger.getGlobal() : loggerIn;
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", BasicRunner.class.getSimpleName(), String.format(format, args)));
        }
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
    
    public static class LogOutputStream extends OutputStream {

        private Logger logger;
        private String prefix;
        private String suffix;
        private StringBuilder sb = new StringBuilder();   

        public LogOutputStream(String prefix, String suffix) {
            this(Logger.getGlobal(), prefix, suffix);
        }

        public LogOutputStream() {
            this(Logger.getGlobal(), null, null);
        }

        public LogOutputStream(Logger logger, String prefix, String suffix) {
            this.logger = logger;
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (b == '\n') {
                if (logger.isLoggable(Level.INFO)) {
                    if (prefix != null && suffix != null) {
                        logger.info(String.format("%s%s%s", prefix, sb, suffix));
                    } else if (prefix != null) {
                        logger.info(String.format("%s%s", prefix, sb));
                    } else if (suffix != null) {
                        logger.info(String.format("%s%s", sb, suffix));
                    } else {
                        logger.info(String.format("%s", sb));
                    }
                }
                sb.setLength(0);
            } else {
                sb.append((char)b);
            }
        }
    }
}

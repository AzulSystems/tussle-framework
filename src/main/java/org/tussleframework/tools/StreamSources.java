/*
 * Copyright (c) 2021-2022, Azul Systems
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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.tussleframework.TussleException;

class StreamSources {

    private StreamSources() {}

    public static interface StreamSource extends Closeable {
        String getName();
        String getAbsName();
        InputStream getStream() throws TussleException;
        Collection<StreamSource> list();
    }

    public static class FileStreamSource implements StreamSource {
        protected FileInputStream fis;
        protected File file;

        public FileStreamSource(File file) {
            this.file = file;
        }

        public InputStream getStream() throws TussleException {
            if (fis == null) {
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new TussleException(e);
                }
            }
            return fis;
        }

        @Override
        public void close() throws IOException {
            if (fis != null) {
                fis.close();
            }
        }

        @Override
        public Collection<StreamSource> list() {
            File[] files = file.listFiles();
            ArrayList<StreamSource> list = new ArrayList<>();
            if (files == null) {
                return list;
            }
            for (File f : files) {
                list.add(new FileStreamSource(f));
            }
            return list;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getAbsName() {
            return file.getAbsolutePath();
        }
    }

    public static class ZipStreamSource implements StreamSource {
        protected ZipFile zipFile;
        protected boolean doClose;
        protected String name;

        public ZipStreamSource(ZipFile zipFile, String name) {
            this.zipFile = zipFile;
            this.name = name;
        }

        public ZipStreamSource(File file) throws TussleException {
            this(file, null);
        }

        public ZipStreamSource(File file, String name) throws TussleException {
            try {
                this.zipFile = new ZipFile(file);
            } catch (Exception e) {
                throw new TussleException(e);
            }
            this.doClose = true;
            this.name = name;
        }

        public InputStream getStream() throws TussleException {
            try {
                ZipEntry zipEntry = zipFile.getEntry(name);
                return zipFile.getInputStream(zipEntry);
            } catch (IOException e) {
                throw new TussleException(e);
            }
        }

        @Override
        public void close() {
            if (zipFile != null && doClose) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    /// ignore
                }
            }
        }

        @Override
        public Collection<StreamSource> list() {
            ArrayList<StreamSource> list = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }
                list.add(new ZipStreamSource(zipFile, zipEntry.getName()));
            }
            return list;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAbsName() {
            File file = new File(zipFile.getName());
            return file.getAbsolutePath() + ":" + name;
        }
    }
}
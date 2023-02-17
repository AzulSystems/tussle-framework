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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

public class FileTool {
    
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("tussle.debug"));

    private FileTool() {
    }

    public static void backupAndCreateDir(File fileDir) {
        if (isFileOrNonEmptyDir(fileDir) && !backupDir(fileDir)) {
            throw new IllegalArgumentException(String.format("Non-empty '%s' already exists and failed to backup", fileDir));
        }
        if (!fileDir.exists() && !fileDir.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create dir '%s'", fileDir));
        }
    }

    public static boolean isFileOrNonEmptyDir(File fileDir) {
        if (fileDir.exists()) {
            if (fileDir.isDirectory()) {
                String[] files = fileDir.list();
                return files != null && files.length > 0;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean backupDir(File fileDir) {
        if (!fileDir.isDirectory()) {
            return false;
        }
        int i = 1;
        File dir = new File(fileDir.getParent(), String.format("%s.%d", fileDir.getName(), i));
        while (dir.exists()) {
            if (i == 1000) {
                return false;
            }
            i++;
            dir = new File(fileDir.getParent(), String.format("%s.%d", fileDir.getName(), i));
        }
        return fileDir.renameTo(dir);
    }

    /**
     * /a/b/c/name.ext -> name.ext
     * 
     * @param fileName
     * @return
     */
    public static String clearPath(String fileName) {
        int pos = fileName.lastIndexOf('/');
        if (pos >= 0) {
            fileName = fileName.substring(pos + 1);
        }
        return fileName;
    }

    /**
     * /a/b/c/name.ext -> name.ext
     * /a/b/c/arch.zip:name.ext -> name.ext
     * /a/b/c/any:name.ext -> name.ext
     * 
     * @param fileName
     * @return
     */
    public static String clearExtPath(String fileName) {
        fileName = clearPath(fileName);
        int pos = fileName.indexOf(":");
        if (pos >= 0) {
            fileName = fileName.substring(pos + 1);
        }
        return fileName;
    }

    /**
     * /a/b/c/name.ext -> name
     *  
     * @param fileName
     * @return
     */
    public static String clearPathAndExtension(String fileName) {
        fileName = clearPath(fileName);
        int pos = fileName.lastIndexOf('.');
        if (pos >= 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    /**
     * /a/b/c/name.ext -> /a/b/c
     * 
     * @param baseDir
     * @param fileName
     * @return
     */
    public static File getBaseDir(String baseDir, String fileName) {
        File file = new File(fileName);
        File dir = baseDir != null && !file.isAbsolute() ? new File(baseDir, fileName).getParentFile() : file.getParentFile();
        if (dir == null) {
            dir = new File(".");
        }
        return dir;
    }

    public static void listFiles(String baseDir, String fileMatch, Collection<File> res) {
        File dir = getBaseDir(baseDir, fileMatch);
        if (DEBUG) {
            LoggerTool.log(FileTool.class.getSimpleName(), "listFiles: baseDir(%s) + fileMatch(%s) => dir(%s)",  baseDir, fileMatch, dir);
        }
        Pattern regexp = Pattern.compile(new File(fileMatch).getName());
        File[] list = dir.listFiles(f -> regexp.matcher(f.getName()).matches());
        if (list != null) {
            Collections.addAll(res, list);
        }
    }

    public static Collection<File> listFiles(String baseDir, Collection<String> fileMatches) {
        ArrayList<File> res = new ArrayList<>();
        fileMatches.forEach(fileMatch -> listFiles(baseDir, fileMatch, res));
        return res;
    }

    public static Collection<File> listFiles(Collection<String> fileMatches) {
        return listFiles(null, fileMatches);
    }
}

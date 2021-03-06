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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tussleframework.TussleException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class JsonTool {

    private JsonTool() {
    }

    public static void printJson(Object obj, String file) throws TussleException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            printJson(obj, out);
        } catch (IOException e) {
            throw new TussleException(e);
        }
    }

    /**
     * Print in json format 
     * 
     */
    public static void printJson(Object obj, OutputStream out) throws TussleException {
        ObjectWriter ow = new ObjectMapper()
                .setSerializationInclusion(Include.NON_NULL)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .writer().withDefaultPrettyPrinter();
        try {
            ow.writeValue(out, obj);
        } catch (Exception e) {
            throw new TussleException(e);
        }
    }
    
    public static <T> T readJson(String file, Class<T> klass) throws TussleException {
        try (FileInputStream in = new FileInputStream(file)) {
            return readJson(in, klass, false, true);
        } catch (IOException e) {
            throw new TussleException(e);
        }
    }

    /**
     * Read object from json file (stream)
     */
    public static <T> T readJson(InputStream in, Class<T> klass) throws TussleException {
        return readJson(in, klass, false, true);
    }

    public static <T> T readJson(InputStream in, Class<T> klass, boolean failOnUnknownProperties, boolean snakeCase) throws TussleException {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
        if (snakeCase) {
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        }
        ObjectReader objectReader = mapper.reader();
        try {
            return objectReader.readValue(in, klass);
        } catch (IOException e) {
            throw new TussleException(e);
        }
    }
}

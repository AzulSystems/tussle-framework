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

package org.tussleframework.metrics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.tussleframework.RunProperties;
import org.tussleframework.TussleException;
import org.tussleframework.tools.Informator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MetricData {
    protected RunProperties runProperties;
    protected ArrayList<Metric> metrics = new ArrayList<>();

    public void add(Metric m) {
        metrics.add(m);
    }

    public void loadRunProperties(String file) throws TussleException {
        ObjectReader or = new ObjectMapper().reader();
        try {
            HashMap<?, ?> props = or.readValue(new File(file), RunProperties.class);
            if (props.containsKey("doc")) {
                Object doc = props.get("doc");
                if (doc instanceof HashMap<?, ?>) {
                    props = (HashMap<?, ?>) doc;
                }
                doc = props.get("runProperties");
                if (doc instanceof HashMap<?, ?>) {
                    props = (HashMap<?, ?>) doc;
                }
                doc = props.get("run_properties");
                if (doc instanceof HashMap<?, ?>) {
                    props = (HashMap<?, ?>) doc;
                }
            }
            runProperties = new RunProperties();
            props.forEach((k, v) -> runProperties.put(k.toString(), v));
        } catch (IOException e) {
            throw new TussleException(e);
        }
    }

    public void loadDefaultRunProperties() {
        runProperties = new RunProperties();
        runProperties.getHardware().putAll(Informator.getHwInfo());
        runProperties.getOs().putAll(Informator.getOsInfo());
        runProperties.getJvm().putAll(Informator.getJvmInfo());
        runProperties.setProperty("testedBy", Informator.getUser());
        runProperties.setProperty("vm_type", Informator.getJvmName());
    }

    public Metric find(String name) {
        return metrics.stream().filter(metric -> metric.name.equals(name)).findFirst().orElse(null);
    }
}

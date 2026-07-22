/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.perf.jmh;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MdcKeyFilterBenchmark {

    private JsonTemplateLayout patternLayout;
    private JsonTemplateLayout keyExcludesLayout;
    private LogEvent logEvent;

    @Setup
    public void setup() {

        SortedArrayStringMap contextData = new SortedArrayStringMap();
        contextData.putValue("userId", "12345");
        contextData.putValue("transactionId", "tx-98765");
        contextData.putValue("userRole", "admin");
        contextData.putValue("@timestamp", "2026-07-22T10:15:30.123Z");
        contextData.putValue("message", "Tx completed");
        contextData.putValue("log.level", "INFO");

        logEvent = Log4jLogEvent.newBuilder().setContextData(contextData).build();

        // Layout using regex pattern
        String patternTemplate = "{" + "\"$resolver\": \"mdc\", "
                + "\"pattern\": \"^(?!@timestamp|message|log\\\\.logger|log\\\\.level|event\\\\.dataset|process\\\\.thread\\\\.name|process\\\\.thread\\\\.id|ecs\\\\.version).*$\""
                + "}";

        patternLayout = JsonTemplateLayout.newBuilder()
                .setConfiguration(new DefaultConfiguration())
                .setEventTemplate(patternTemplate)
                .build();

        // Layout using keyExcludes HashSet
        String keyExcludesTemplate = "{" + "\"$resolver\": \"mdc\", "
                + "\"keyExcludes\": [\"@timestamp\", \"message\", \"log.logger\", \"log.level\", \"event.dataset\", \"process.thread.name\", \"process.thread.id\", \"ecs.version\"]"
                + "}";

        keyExcludesLayout = JsonTemplateLayout.newBuilder()
                .setConfiguration(new DefaultConfiguration())
                .setEventTemplate(keyExcludesTemplate)
                .build();
    }

    @Benchmark
    public String testPatternResolver() {
        return patternLayout.toSerializable(logEvent);
    }

    @Benchmark
    public String testKeyExcludesResolver() {
        return keyExcludesLayout.toSerializable(logEvent);
    }
}

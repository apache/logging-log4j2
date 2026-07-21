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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MdcKeyFilterBenchmark {

    private static final String[] MDC_KEYS = {
        "userId", "transactionId", "userRole",
        "@timestamp", "message", "log.level"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "^(?!@timestamp|message|log\\.logger|log\\.level|event\\.dataset|process\\.thread\\.name|process\\.thread\\.id|ecs\\.version).*$");

    private static final Set<String> EXCLUDED_KEYS = new HashSet<>(Arrays.asList(
            "@timestamp",
            "message",
            "log.logger",
            "log.level",
            "event.dataset",
            "process.thread.name",
            "process.thread.id",
            "ecs.version"));

    @Benchmark
    public int testRegex() {
        int allowedCount = 0;
        for (String key : MDC_KEYS) {
            if (PATTERN.matcher(key).matches()) {
                allowedCount++;
            }
        }
        return allowedCount;
    }

    @Benchmark
    public int testKeyExcludes() {
        int allowedCount = 0;
        for (String key : MDC_KEYS) {
            if (!EXCLUDED_KEYS.contains(key)) {
                allowedCount++;
            }
        }
        return allowedCount;
    }
}

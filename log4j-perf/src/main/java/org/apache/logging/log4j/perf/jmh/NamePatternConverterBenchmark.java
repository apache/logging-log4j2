/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.perf.jmh;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.LoggerPatternConverter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Tests Log4j2 NamePatternConverter's performance.<br>
 *
 * How to run these benchmarks:<br>
 *
 * Single thread:<br>
 * <pre>java -jar log4j-perf/target/benchmarks.jar ".*NamePatternConverterBenchmark.*" -f 1 -wi 2 -i 3 -r 3s -jvmArgs '-server -XX:+AggressiveOpts'</pre>
 *
 * Multiple threads (for example, 4 threads):<br>
 * <pre>java -jar log4j-perf/target/benchmarks.jar ".*NamePatternConverterBenchmark.*" -f 1 -wi 4 -i 20 -t 4 -si true</pre>
 *
 * Usage help:<br>
 * <pre>java -jar log4j-perf/target/benchmarks.jar -help</pre>
 */
@Fork(1)
@Threads(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 4, time = 3)
public class NamePatternConverterBenchmark {

    @State(Scope.Benchmark)
    public static class ExecutionPlan {
        @Param({
                "org.bogus.hokus.pokus.org.bogus.hokus.pokus.org.bogus.hokus.pokus.RetroEncabulatorFactorySingleton",
                "org.bogus.hokus.pokus.Clazz1",
                "com.bogus.hokus.pokus.Clazz2",
                "edu.bogus.hokus.pokus.a.Clazz3",
                "de.bogus.hokus.b.Clazz4",
                "jp.bogus.c.Clazz5",
                "cn.d.Clazz6"
        })
        String className;
        LogEvent event;
        private final ThreadLocal<StringBuilder> destination = ThreadLocal.withInitial(StringBuilder::new);

        final LoggerPatternConverter converter = LoggerPatternConverter.newInstance(new String[] {"1."});

        @Setup
        public void setup() {
            event = new BenchmarkLogEvent(className);
        }

        StringBuilder destination() {
            StringBuilder result = destination.get();
            result.setLength(0);
            return result;
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void benchNamePatternConverter(ExecutionPlan plan) {
        plan.converter.format(plan.event, plan.destination());
    }

    private static class BenchmarkLogEvent extends AbstractLogEvent {
        private final String loggerName;

        BenchmarkLogEvent(String loggerName) {
            this.loggerName = loggerName;
        }

        @Override
        public String getLoggerName() {
            return loggerName;
        }
    }

}
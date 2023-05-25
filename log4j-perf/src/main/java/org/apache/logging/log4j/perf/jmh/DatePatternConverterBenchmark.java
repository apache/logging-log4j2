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

import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Compares {@link DatePatternConverter} formatting and caching efficiency.
 * <p>
 * HOW TO RUN THIS TEST
 * </p>
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*DatePatternConverterBenchmark.*" -p useThreadlocals=true,false
 * </pre>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(Threads.MAX)
public class DatePatternConverterBenchmark {

    @Param({ "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.nnnnnn" })
    private String pattern;

    @Param({ "true", "false" })
    private boolean useThreadlocals;

    private static final ThreadLocal<MutableInstant> INSTANT = ThreadLocal.withInitial(MutableInstant::new);
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = ThreadLocal.withInitial(StringBuilder::new);

    private Clock clock;
    private DatePatternConverter converter;

    @Setup
    public void setup() {
        System.setProperty("log4j2.enableThreadlocals", useThreadlocals ? "true" : "false");
        clock = ClockFactory.getClock();
        converter = DatePatternConverter.newInstance(new String[] { pattern });
    }

    @Benchmark
    public void formatInstant(final Blackhole blackhole) {
        final StringBuilder sb = STRING_BUILDER.get();
        final MutableInstant instant = INSTANT.get();
        instant.initFrom(clock);
        sb.setLength(0);
        converter.format(instant, sb);
        blackhole.consume(sb.length());
    }

    @Benchmark
    public void initInstant() {
        final MutableInstant instant = INSTANT.get();
        instant.initFrom(clock);
    }

}

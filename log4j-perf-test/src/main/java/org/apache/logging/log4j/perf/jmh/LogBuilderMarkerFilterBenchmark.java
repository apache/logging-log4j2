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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * <p>
 * Compares {@link Logger} and {@link LogBuilder} in the presence or absence of
 * a global filter. In the absence of a global filter ({@code -Dnofilter}) the
 * {@link Logger} can return a no-op {@link LogBuilder} if the level is
 * disabled. No such an optimization is possible in the presence of a global
 * filter.
 * </p>
 * <p>
 * HOW TO RUN THIS TEST
 * </p>
 * <ul>
 * <li>single thread:
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*LogBuilderMarkerFilterBenchmark.*" -p useFilter=true,false
 * </pre>
 *
 * </li>
 * <li>multiple threads (for example, 4 threads):
 *
 * <pre>
 * java -jar target/benchmarks.jar ".*LogBuilderMarkerFilterBenchmark.*" -p useFilter=true,false -t 4
 * </pre>
 *
 * </li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class LogBuilderMarkerFilterBenchmark {

    @Param("target/logbuilder.benchmark.log")
    private String fileName;

    @Param("true")
    private boolean useFilter;

    private static final String MESSAGE = "This is a test!";
    private Marker marker;
    private Logger logger;

    @Setup
    public void setUp() {
        System.setProperty("logbuilder.benchmark.output", fileName);
        if (useFilter) {
            System.setProperty("log4j2.configurationFile", "log4j2-markerFilter-perf2.xml");
        } else {
            System.setProperty("log4j2.configurationFile", "log4j2-noFilter-perf.xml");
        }
        logger = LogManager.getLogger(getClass());
        marker = MarkerManager.getMarker("TestMarker");
    }

    @TearDown
    public void tearDown() throws IOException {
        System.clearProperty("log4j2.configurationFile");
        LogManager.shutdown();
        final Path filePath = Paths.get(fileName);
        if (Files.isRegularFile(filePath)) {
            Files.deleteIfExists(filePath);
        }
    }

    @Benchmark
    public void loggerTraceMarker() {
        logger.trace(marker, MESSAGE);
    }

    @Benchmark
    public void loggerInfoNoMarker() {
        logger.info(MESSAGE);
    }

    @Benchmark
    public void loggerTraceNoMarker() {
        logger.trace(MESSAGE);
    }

    @Benchmark
    public void logBuilderTraceMarker() {
        logger.atTrace().withMarker(marker).log(MESSAGE);
    }

    @Benchmark
    public void logBuilderInfoNoMarker() {
        logger.atInfo().log(MESSAGE);
    }

    @Benchmark
    public void logBuilderTraceNoMarker() {
        logger.atTrace().log(MESSAGE);
    }
}

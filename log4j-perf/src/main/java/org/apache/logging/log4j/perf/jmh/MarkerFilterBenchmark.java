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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * Benchmarks Log4j 2 and Logback using a global MarkerFilter which will not be matched for this test. The Marker
 * provided will have a
 */
// HOW TO RUN THIS TEST
// single thread:
// java -jar target/benchmarks.jar ".*MarkerFilterBenchmark.*" -f 1 -i 5 -wi 5 -bm sample -tu ns
// multiple threads (for example, 4 threads):
// java -jar target/benchmarks.jar ".*MarkerFilterBenchmark.*" -f 1 -i 5 -wi 5 -t 4 -si true -bm sample -tu ns
@State(Scope.Benchmark)
public class MarkerFilterBenchmark {
    Logger log4jLogger;
    org.slf4j.Logger slf4jLogger;
    org.slf4j.Marker LOGBACK_FLOW_MARKER;
    org.slf4j.Marker LOGBACK_ENTRY_MARKER;
    Marker LOG4J_FLOW_MARKER;
    Marker LOG4J_ENTRY_MARKER;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-markerFilter-perf.xml");
        System.setProperty("logback.configurationFile", "logback-markerFilter-perf.xml");
        LOGBACK_FLOW_MARKER = MarkerFactory.getMarker("FLOW");
        LOGBACK_ENTRY_MARKER = MarkerFactory.getMarker("ENTRY");
        LOG4J_FLOW_MARKER = MarkerManager.getMarker("FLOW");
        LOG4J_ENTRY_MARKER = MarkerManager.getMarker("ENTRY");
        LOGBACK_ENTRY_MARKER.add(LOGBACK_FLOW_MARKER);
        LOG4J_ENTRY_MARKER.addParents(LOG4J_FLOW_MARKER);
        log4jLogger = LogManager.getLogger(MarkerFilterBenchmark.class);
        slf4jLogger = LoggerFactory.getLogger(MarkerFilterBenchmark.class);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        System.clearProperty("log4j.configuration");
        System.clearProperty("logback.configurationFile");
    }

    @Benchmark
    public boolean baseline() {
        return true;
    }

    @Benchmark
    public void log4jParentMarker() {
        log4jLogger.info(LOG4J_ENTRY_MARKER, "This is a test");
    }

    @Benchmark
    public void log4jSimpleMarker() {
        log4jLogger.info(LOG4J_FLOW_MARKER, "This is a test");
    }

    @Benchmark
    public void log4jTooFine() {
        log4jLogger.trace("This is not logged");
    }

    @Benchmark
    public void logbackParentMarker() {
        slf4jLogger.info(LOGBACK_ENTRY_MARKER, "This is a test");
    }

    @Benchmark
    public void logbackSimpleMarker() {
        slf4jLogger.info(LOGBACK_FLOW_MARKER, "This is a test");
    }

    @Benchmark
    public void logbackTooFine() {
        slf4jLogger.trace("This is not logged");
    }
}

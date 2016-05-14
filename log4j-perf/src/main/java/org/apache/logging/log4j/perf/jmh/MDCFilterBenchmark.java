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
import org.apache.logging.log4j.ThreadContext;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Benchmarks Log4j 2 and Logback ThreadContext/MDC Filter performance.
 */
// HOW TO RUN THIS TEST
// single thread:
// java -jar target/benchmarks.jar ".*MDCFilterBenchmark.*" -f 1 -i 5 -wi 5 -bm sample -tu ns
//
// multiple threads (for example, 4 threads):
// java -jar target/benchmarks.jar ".*MDCFilterBenchmark.*" -f 1 -i 5 -wi 5 -t 4 -si true -bm sample -tu ns

@State(Scope.Thread)
public class MDCFilterBenchmark {
    Logger log4jLogger;
    org.slf4j.Logger slf4jLogger;
    Integer j;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-threadContextFilter-perf.xml");
        System.setProperty("logback.configurationFile", "logback-mdcFilter-perf.xml");
        ThreadContext.put("user", "Apache");
        MDC.put("user", "Apache");

        log4jLogger = LogManager.getLogger(MDCFilterBenchmark.class);
        slf4jLogger = LoggerFactory.getLogger(MDCFilterBenchmark.class);
        j = Integer.valueOf(2);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        System.clearProperty("logback.configurationFile");
    }

    @Benchmark
    public boolean baseline() {
        return true;
    }

    @Benchmark
    public void log4jThreadContextFilter() {
        log4jLogger.info("This is a test");
    }

    @Benchmark
    public void slf4jMDCFilter() {
        slf4jLogger.info("This is a test");
    }

}

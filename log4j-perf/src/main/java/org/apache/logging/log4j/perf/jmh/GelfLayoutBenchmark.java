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
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks Log4j 2 GelfLayout with a FileAppender.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar GelfLayoutBenchmark -f 1 -i 5 -wi 5 -bm sample -tu ns
@State(Scope.Thread)
public class GelfLayoutBenchmark {
    public static final String MESSAGE =
            "This is rather long and chatty log message with quite some interesting information and a bit of fun in it which is suitable here";

    Logger log4j2Logger;
    org.apache.log4j.Logger log4j1Logger;
    int j;

    @Setup
    public void setUp() {
        System.setProperty("log4j.configurationFile", "log4j2-gelf-perf.xml");

        File log4j2File = new File("target/testlog4j2.json");
        log4j2File.delete();

        log4j2Logger = LogManager.getLogger(GelfLayoutBenchmark.class);
        j = 0;
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");

        File log4j2File = new File("target/testlog4j2.json");
        log4j2File.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public boolean baseline() {
        ++j;
        return true;
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void log4j2Gelf() {
        log4j2Logger.debug(MESSAGE);
    }

}

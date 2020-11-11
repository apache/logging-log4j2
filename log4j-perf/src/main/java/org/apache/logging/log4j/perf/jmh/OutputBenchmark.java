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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmarks Log4j 2, Log4j 1, Logback and JUL using the DEBUG level which is enabled for this test. The configuration
 * for each uses a FileAppender
 */
// HOW TO RUN THIS TEST
// java -jar log4j-perf/target/benchmarks.jar ".*OutputBenchmark.*" -f 1 -wi 10 -i 20
//
// RUNNING THIS TEST WITH 4 THREADS:
// java -jar log4j-perf/target/benchmarks.jar ".*OutputBenchmark.*" -f 1 -wi 10 -i 20 -t 4
@State(Scope.Thread)
public class OutputBenchmark {
    public static final String MESSAGE = "This is a debug message";

    Logger log4j2Logger;


    @State(Scope.Group)
    public static class Redirect {
        PrintStream defaultStream = System.out;

        @Setup
        public void setUp() throws Exception {
            PrintStream ps = new PrintStream(new FileOutputStream("target/stdout.log"));
            System.setOut(ps);
        }

        @TearDown
        public void tearDown() {
            PrintStream ps = System.out;
            System.setOut(defaultStream);
            ps.close();
        }
    }

    @Setup
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-perf3.xml");

        deleteLogFiles();

        log4j2Logger = LogManager.getLogger(OutputBenchmark.class);
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");

        deleteLogFiles();
    }

    private void deleteLogFiles() {
        final File outFile = new File("target/stdout.log");
        final File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
        outFile.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Group("console")
    @Benchmark
    public void console() {
        System.out.println(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Group("file")
    @Benchmark
    public void log4j2File() {
        log4j2Logger.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Group("redirect")
    @Benchmark
    public void redirect(Redirect redirect) {
        System.out.println(MESSAGE);
    }
}

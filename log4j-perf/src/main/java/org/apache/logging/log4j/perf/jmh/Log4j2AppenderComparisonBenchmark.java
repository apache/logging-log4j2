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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmarks Log4j 2's Console, File, RandomAccessFile, MemoryMappedFile and Rewrite appender.
 */
// HOW TO RUN THIS TEST
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderComparisonBenchmark.*" -f 1 -wi 10 -i 20
//
// RUNNING THIS TEST WITH 4 THREADS:
// java -jar log4j-perf/target/benchmarks.jar ".*Log4j2AppenderComparisonBenchmark.*" -f 1 -wi 10 -i 20 -t 4
@State(Scope.Thread)
public class Log4j2AppenderComparisonBenchmark {
    public static final String MESSAGE = "Short message";

    private Logger file;
    private Logger raf;
    private Logger mmap;
    private Logger console;
    private Logger noop;
    private Logger rewrite;

    @Setup
    public void setUp() throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2-appenderComparison.xml");
        deleteLogFiles();

        file = LogManager.getLogger("FileLogger");
        raf = LogManager.getLogger("RAFLogger");
        mmap = LogManager.getLogger("MMapLogger");
        console = LogManager.getLogger("ConsoleLogger");
        noop = LogManager.getLogger("NoopLogger");
        rewrite = LogManager.getLogger("RewriteLogger");
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j.configurationFile");
        deleteLogFiles();
    }

    private void deleteLogFiles() {
        File log4j2File = new File ("target/testlog4j2.log");
        log4j2File.delete();
        File log4jRandomFile = new File ("target/testRandomlog4j2.log");
        log4jRandomFile.delete();
        File mmapFile = new File ("target/MemoryMappedFileAppenderTest.log");
        mmapFile.delete();
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void raf() {
        raf.debug(MESSAGE);
    }


    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void file() {
        file.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void mmap() {
        mmap.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void noop() {
        noop.debug(MESSAGE);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void rewrite() {
        rewrite.debug(MESSAGE);
    }
}

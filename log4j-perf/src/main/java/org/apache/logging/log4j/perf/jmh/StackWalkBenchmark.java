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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.perf.util.StackDriver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark logging with logging disabled.
 * // ============================== HOW TO RUN THIS TEST: ====================================
 * //
 * // single thread:
 * // java -jar log4j-perf/target/benchmarks.jar ".*StackWalkBenchmark.*" -f 1 -wi 5 -i 10
 * //
 * // multiple threads (for example, 4 threads):
 * // java -jar log4j-perf/target/benchmarks.jar ".*StackWalkBenchmark.*" -f 1 -wi 5 -i 10 -t 4 -si true
 * //
 * // Usage help:
 * // java -jar log4j-perf/target/benchmarks.jar -help
 * //
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class StackWalkBenchmark {

    private static final StackDriver stackDriver = new StackDriver();
    private final static ThreadLocal<String> FQCN = new ThreadLocal<>();
    private final static FqcnCallerLocator LOCATOR = new FqcnCallerLocator();
    private final static StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Param({"10", "20", "50"})
    private int initialDepth;

    @Param({"5", "10", "20"})
    private int callDepth;

    @Benchmark
    public void throwableSearch(Blackhole bh)  {

        stackDriver.deepCall(initialDepth, callDepth, fqcn -> {
            final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            boolean found = false;
            for (int i = 0; i < stackTrace.length; i++) {
                final String className = stackTrace[i].getClassName();
                if (fqcn.equals(className)) {
                    found = true;
                    continue;
                }
                if (found  && !fqcn.equals(className)) {
                    return stackTrace[i];
                }
            }
            return null;
        });
    }

    @Benchmark
    public void stackWalkerWalk(Blackhole bh) {
        stackDriver.deepCall(initialDepth, callDepth, fqcn -> walker.walk(
                s -> s.dropWhile(f -> !f.getClassName().equals(fqcn)) // drop the top frames until we reach the logger
                        .dropWhile(f -> f.getClassName().equals(fqcn)) // drop the logger frames
                        .findFirst())
                .get()
                .toStackTraceElement());
    }

    @Benchmark
    public void stackWalkerArray(Blackhole bh)  {

        stackDriver.deepCall(initialDepth, callDepth, fqcn -> {
            FQCN.set(fqcn);
            final StackWalker.StackFrame walk = walker.walk(LOCATOR);
            final StackTraceElement element = walk == null ? null : walk.toStackTraceElement();
            FQCN.set(null);
            return element;
        });
    }

    @Benchmark
    public void baseline(Blackhole bh)  {

        stackDriver.deepCall(initialDepth, callDepth, fqcn -> null);
    }

    static final class FqcnCallerLocator implements Function<Stream<StackWalker.StackFrame>, StackWalker.StackFrame> {

        @Override
        public StackWalker.StackFrame apply(Stream<StackWalker.StackFrame> stackFrameStream) {
            String fqcn = FQCN.get();
            boolean foundFqcn = false;
            Object[] frames = stackFrameStream.toArray();
            for (int i = 0; i < frames.length ; ++i) {
                final String className = ((StackWalker.StackFrame) frames[i]).getClassName();
                if (!foundFqcn) {
                    // Skip frames until we find the FQCN
                    foundFqcn = className.equals(fqcn);
                } else if (!className.equals(fqcn)) {
                    // The frame is no longer equal to the FQCN so it is the one we want.
                    return (StackWalker.StackFrame) frames[i];
                } // Otherwise it is equal to the FQCN so we need to skip it.
            }
            // Should never happen
            return null;
        }
    }

}

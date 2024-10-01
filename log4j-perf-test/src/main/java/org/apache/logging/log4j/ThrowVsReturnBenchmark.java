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
package org.apache.logging.log4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class ThrowVsReturnBenchmark {

    private static final int SALT = ThreadLocalRandom.current().nextInt();

    private static final RuntimeException EXCEPTION = new RuntimeException();

    private static final int MAX_DEPTH = 5;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int withReturn(final Blackhole blackhole) {
        return withReturn(blackhole, 0);
    }

    private static int withReturn(final Blackhole blackhole, final int depth) {
        doWork(blackhole);
        return depth < MAX_DEPTH ? withReturn(blackhole, depth + 1) : blackhole.i1;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int withThrow(final Blackhole blackhole) {
        try {
            withThrow(blackhole, 0);
        } catch (final Exception error) {
            if (error == EXCEPTION) {
                return blackhole.i1;
            }
        }
        throw new IllegalStateException();
    }

    private static void withThrow(final Blackhole blackhole, final int depth) {
        doWork(blackhole);
        if (depth < MAX_DEPTH) {
            withThrow(blackhole, depth + 1);
        } else {
            throw EXCEPTION;
        }
    }

    private static void doWork(final Blackhole blackhole) {
        for (int i = 0; i < 1_000; i++) {
            blackhole.consume((i << 10) + SALT);
        }
    }
}

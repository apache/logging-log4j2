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

import java.util.concurrent.ConcurrentHashMap;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Compares performance of ThreadLocal, vs ConcurrentHashMap&lt;Thread, Object&gt;.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsConcurrentHashMap.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
//
// four threads:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsConcurrentHashMap.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
// -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ThreadLocalVsConcurrentHashMapBenchmark {

    private static final String VALUE = "value";
    private static ConcurrentHashMap<Thread, StringBuilder> map = new ConcurrentHashMap<>();
    private static ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

    @Benchmark
    public String newInstance() {
        final StringBuilder sb = getNew();
        sb.append(VALUE);
        return sb.toString();
    }

    @Benchmark
    public String threadLocal() {
        final StringBuilder sb = getThreadLocal();
        sb.append(VALUE);
        return sb.toString();
    }

    @Benchmark
    public String concurrentHashMap() {
        final StringBuilder sb = getConcurrentMap();
        sb.append(VALUE);
        return sb.toString();
    }

    private StringBuilder getNew() {
        final StringBuilder buf = new StringBuilder();
        return buf;
    }

    private StringBuilder getThreadLocal() {
        StringBuilder buf = threadLocal.get();
        if (buf == null) {
            buf = new StringBuilder();
            threadLocal.set(buf);
        }
        buf.setLength(0);
        return buf;
    }

    private StringBuilder getConcurrentMap() {
        StringBuilder buf = map.get(Thread.currentThread());
        if (buf == null) {
            buf = new StringBuilder();
            map.put(Thread.currentThread(), buf);
        }
        buf.setLength(0);
        return buf;
    }
}
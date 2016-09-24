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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextBenchmarkAccess;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.perf.nogc.OpenHashStringMap;
import org.apache.logging.log4j.spi.CopyOnWriteOpenHashMapThreadContextMap;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.GarbageFreeOpenHashMapThreadContextMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compares performance of ThreadContextMap implementations.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadContextBench.*"
//
// four threads:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadContextBench.*" -f 1 -wi 10 -i 20 -tu ns -bm sample -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ThreadContextBenchmark {
    private static final String DEFAULT_CONTEXT_MAP = "Default";
    private static final String COPY_OPENHASH_MAP = "CopyOpenHash";
    private static final String COPY_ARRAY_MAP = "CopySortedArray";
    private static final String NO_GC_OPENHASH_MAP = "NoGcOpenHash";
    private static final String NO_GC_ARRAY_MAP = "NoGcSortedArray";
    private static final Map<String, Class<? extends ThreadContextMap>> IMPLEMENTATIONS = new HashMap<>();
    static {
        IMPLEMENTATIONS.put(DEFAULT_CONTEXT_MAP, DefaultThreadContextMap.class);
        IMPLEMENTATIONS.put(COPY_OPENHASH_MAP, CopyOnWriteOpenHashMapThreadContextMap.class);
        IMPLEMENTATIONS.put(COPY_ARRAY_MAP, CopyOnWriteOpenHashMapThreadContextMap.SUPER); //CopyOnWriteSortedArrayThreadContextMap.class);
        IMPLEMENTATIONS.put(NO_GC_OPENHASH_MAP, GarbageFreeOpenHashMapThreadContextMap.class);
        IMPLEMENTATIONS.put(NO_GC_ARRAY_MAP, GarbageFreeOpenHashMapThreadContextMap.SUPER); //GarbageFreeSortedArrayThreadContextMap.class);
    }

    @Param({ "Default", "CopyOpenHash", "CopySortedArray", "NoGcOpenHash", "NoGcSortedArray"})
    //@Param({ "Default", }) // for legecyInject benchmarks
    public String threadContextMapAlias;

    @Param({"5", "50", "500"})
    public int count;

    private final int KEY_LENGTH = 16;
    private String[] keys;
    private String[] values;
    private List<Property> propertyList;

    private ContextDataInjector injector;
    private StringMap reusableContextData;

    @Setup
    public void setup() {
        System.setProperty("log4j2.threadContextMap", IMPLEMENTATIONS.get(threadContextMapAlias).getName());
        ThreadContextBenchmarkAccess.init();

        injector = ContextDataInjectorFactory.createInjector();
        System.out.println(threadContextMapAlias + ": Injector = " + injector);

        reusableContextData = threadContextMapAlias.contains("Array")
                ? new SortedArrayStringMap()
                : new OpenHashStringMap<>();

        keys = new String[count];
        values = new String[count];
        final Random r = new Random();
        for (int j = 0; j < keys.length; j++) {
            final char[] str = new char[KEY_LENGTH];
            for (int i = 0; i < str.length; i++) {
                str[i] = (char) r.nextInt();
            }
            keys[j] = new String(str);
            values[j] = new String(str);
        }
        final int PROPERTIES_COUNT = 5; // count
        propertyList = new ArrayList<>(PROPERTIES_COUNT);
        for (int j = 0; j < PROPERTIES_COUNT; j++) {
            final char[] str = new char[KEY_LENGTH];
            for (int i = 0; i < str.length; i++) {
                str[i] = (char) r.nextInt();
            }
            propertyList.add(Property.createProperty(new String(str), new String(str)));
        }

        clearAndPut(); // ensure ThreadContext contains values
    }

    @TearDown
    public void tearDown() {
        System.clearProperty("log4j2.threadContextMap");
        ThreadContextBenchmarkAccess.init();
    }

    public void clearAndPut() {
        ThreadContext.clearMap();
        for (int i = 0; i < count; i++) {
            ThreadContext.put(keys[i], values[i]);
        }
    }

    @Benchmark
    public Object get() {
        return ThreadContext.get(keys[count - 1]);
    }

    @Benchmark
    public void putAndRemove() {
        ThreadContext.put("someKey", "someValue");
        ThreadContext.remove("someKey");
    }

    @Benchmark
    public StringMap injectWithoutProperties() {
        reusableContextData.clear();
        return injector.injectContextData(null, reusableContextData);
    }

    @Benchmark
    public StringMap injectWithProperties() {
        reusableContextData.clear();
        return injector.injectContextData(propertyList, reusableContextData);
    }

    @Benchmark
    public Map<String, String> legacyInjectWithoutProperties() {
        return createMap(null);
    }

    @Benchmark
    public Map<String, String> legacyInjectWithProperties() {
        return createMap(propertyList);
    }

    // from Log4jLogEvent::createMap
    static Map<String, String> createMap(final List<Property> properties) {
        final Map<String, String> contextMap = ThreadContext.getImmutableContext();
        if (properties == null || properties.isEmpty()) {
            return contextMap; // may be ThreadContext.EMPTY_MAP but not null
        }
        final Map<String, String> map = new HashMap<>(contextMap);

        for (final Property prop : properties) {
            if (!map.containsKey(prop.getName())) {
                map.put(prop.getName(), prop.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
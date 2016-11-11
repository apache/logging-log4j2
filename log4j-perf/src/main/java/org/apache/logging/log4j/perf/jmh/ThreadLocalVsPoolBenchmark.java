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

import java.nio.charset.Charset;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Checks PatternLayout performance when reusing the StringBuilder in a ThreadLocal, an ObjectPool or when creating a
 * new instance for each log event.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsPool.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
//
// four threads:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsPool.*" -f 1 -wi 10 -i 20 -tu ns -bm sample -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ThreadLocalVsPoolBenchmark {

    static final Charset CHARSET_DEFAULT = Charset.defaultCharset();
    static final String LOG4JPATTERN = "%d %5p [%t] %c{1} %X{transactionId} - %m%n";
    static final int DEFAULT_STRING_BUILDER_SIZE = 1024;

    /**
     * The LogEvent to serialize.
     */
    private final static LogEvent LOG4J2EVENT = createLog4j2Event();

    /**
     * Initial converter for pattern.
     */
    private final static PatternFormatter[] formatters = createFormatters();

    private final StringBuilderPool pool = new StringBuilderPool(DEFAULT_STRING_BUILDER_SIZE);
    private static ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();

    /**
     */
    private static PatternFormatter[] createFormatters() {
        final Configuration config = new DefaultConfiguration();
        final PatternParser parser = new PatternParser(config, "Converter", LogEventPatternConverter.class);
        final List<PatternFormatter> result = parser.parse(LOG4JPATTERN, false, true);
        return result.toArray(new PatternFormatter[result.size()]);
    }

    @Benchmark
    public byte[] newInstance() {
        return serializeWithNewInstance(LOG4J2EVENT).getBytes(CHARSET_DEFAULT);
    }

    @Benchmark
    public byte[] threadLocal() {
        return serializeWithThreadLocal(LOG4J2EVENT).getBytes(CHARSET_DEFAULT);
    }

    @Benchmark
    public byte[] objectPool() {
        return serializeWithPool(LOG4J2EVENT).getBytes(CHARSET_DEFAULT);
    }

    @Benchmark
    public String _stringNewInstance() {
        return serializeWithNewInstance(LOG4J2EVENT);
    }

    @Benchmark
    public String _stringThreadLocal() {
        return serializeWithThreadLocal(LOG4J2EVENT);
    }

    @Benchmark
    public String _stringObjectPool() {
        return serializeWithPool(LOG4J2EVENT);
    }

    private String serializeWithNewInstance(final LogEvent event) {
        final StringBuilder buf = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        return serialize(event, buf);
    }

    private String serializeWithThreadLocal(final LogEvent event) {
        StringBuilder buf = threadLocal.get();
        if (buf == null) {
            buf = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocal.set(buf);
        }
        buf.setLength(0);
        return serialize(event, buf);
    }

    private String serializeWithPool(final LogEvent event) {
        final StringBuilder buf = pool.borrowObject();
        try {
            buf.setLength(0);
            return serialize(event, buf);
        } finally {
            pool.returnObject(buf);
        }
    }

    private String serialize(final LogEvent event, final StringBuilder buf) {
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buf);
        }
        return buf.toString();
    }

    private static LogEvent createLog4j2Event() {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
        final String STR = "AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
        final Message message = new SimpleMessage(STR);
        final Throwable t = null;
        final Map<String, String> mdc = null;
        final ContextStack ndc = null;
        final String threadName = null;
        final StackTraceElement location = null;
        final long timestamp = 12345678;

        return Log4jLogEvent.newBuilder() //
                .setLoggerName("name(ignored)") //
                .setMarker(marker) //
                .setLoggerFqcn(fqcn) //
                .setLevel(level) //
                .setMessage(message) //
                .setThrown(t) //
                .setContextMap(mdc) //
                .setContextStack(ndc) //
                .setThreadName(threadName) //
                .setSource(location) //
                .setTimeMillis(timestamp) //
                .build();
    }
}

/**
 * 
 */
abstract class ObjectPool<T> {
    private final Deque<T> pool = new ConcurrentLinkedDeque<>();

    public T borrowObject() {
        final T object = pool.poll();
        return object == null ? createObject() : object;
    }

    public void returnObject(final T object) {
        pool.add(object);
    }

    protected abstract T createObject();
}

/**
 * 
 */
class StringBuilderPool extends ObjectPool<StringBuilder> {
    private final int initialSize;

    public StringBuilderPool(final int stringBuilderSize) {
        this.initialSize = stringBuilderSize;
    }

    @Override
    public void returnObject(final StringBuilder stringBuilder) {
        stringBuilder.setLength(0);
        super.returnObject(stringBuilder);
    }

    @Override
    protected StringBuilder createObject() {
        return new StringBuilder(initialSize);
    }
}

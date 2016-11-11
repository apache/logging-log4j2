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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.StringBuilderEncoder;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests Log4j2 StringBuilderEncoder performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -Dfile.encoding=ISO-8859-1 -Dlog4j2.is.webapp=false -Dlog4j2.enable.threadlocals=true -jar log4j-perf/target/benchmarks.jar ".*StringBuilderEncoder.*" -f 1 -wi 5 -i 10
// java -Dfile.encoding=UTF8 -Dlog4j2.is.webapp=false -Dlog4j2.enable.threadlocals=true -jar log4j-perf/target/benchmarks.jar ".*StringBuilderEncoder.*" -f 1 -wi 5 -i 10
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class TextEncoderHelperBenchmark {

    final static String STR = "AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
    final static String STR_TEXT = "20:01:59.9876 INFO [org.apache.logging.log4j.perf.jmh.TextEncoderHelperBenchmark] AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
    final static StringBuilder BUFF_TEXT = new StringBuilder(STR_TEXT);
    final static CharBuffer CHAR_BUFFER = CharBuffer.wrap(STR.toCharArray());

    final static LogEvent EVENT = createLogEvent();
    private static final Charset CHARSET_DEFAULT = Charset.defaultCharset();
    private final PatternLayout PATTERN_M_C_D = PatternLayout.createLayout("%d %c %m%n", null, null, null, CHARSET_DEFAULT, false, true, null, null);
    private final Destination destination = new Destination();

    class Destination implements ByteBufferDestination {
        long count = 0;
        ByteBuffer buffer = ByteBuffer.wrap(new byte[256*1024]);

        @Override
        public ByteBuffer getByteBuffer() {
            return buffer;
        }

        @Override
        public ByteBuffer drain(final ByteBuffer buf) {
            buf.flip();
            count += buf.limit();
            buf.clear();
            return buf;
        }
    }

    private static LogEvent createLogEvent() {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
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

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytes() {
        return STR_TEXT.getBytes();
    }

    //private static final ThreadLocal<StringBuilderEncoder> textEncoderHelper = new ThreadLocal<>();
    private final StringBuilderEncoder textEncoderHelper = new StringBuilderEncoder(CHARSET_DEFAULT);
    private StringBuilderEncoder getEncoder() {
        final StringBuilderEncoder result = textEncoderHelper;
        return result;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long textEncoderEncode() {
        final StringBuilderEncoder helper = getEncoder();
        helper.encode(BUFF_TEXT, destination);

        return destination.count;
    }

//    @Benchmark
//    @BenchmarkMode(Mode.SampleTime)
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    public long charBufferEncode() {
//        final StringBuilderEncoder helper = getEncoder();
//        CHAR_BUFFER.limit(CHAR_BUFFER.capacity());
//        CHAR_BUFFER.position(0);
//        helper.encode(CHAR_BUFFER, destination);
//
//        return destination.count;
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SampleTime)
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    public long charBufferCopyAndEncode() {
//        final StringBuilderEncoder helper = getEncoder();
//        CHAR_BUFFER.clear();
//        CHAR_BUFFER.put(STR);
//        CHAR_BUFFER.flip();
//        helper.encode(CHAR_BUFFER, destination);
//
//        return destination.count;
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SampleTime)
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    public long textHelperCopyAndEncode() {
//        final StringBuilderEncoder helper = getEncoder();
//        CHAR_BUFFER.clear();
//        copy(BUFF_TEXT, 0, CHAR_BUFFER);
//        CHAR_BUFFER.flip();
//        helper.encode(CHAR_BUFFER, destination);
//
//        return destination.count;
//    }

    /**
     * Copies characters from the CharSequence into the CharBuffer,
     * starting at the specified offset and ending when either all
     * characters have been copied or when the CharBuffer is full.
     *
     * @return the number of characters that were copied
     */
    private static int copy(final StringBuilder source, final int offset, final CharBuffer destination) {
        final int length = Math.min(source.length() - offset, destination.remaining());
        final char[] array = destination.array();
        final int start = destination.position();
        source.getChars(offset, offset+length, array, start);
        destination.position(start + length);
        return length;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long byteArrayMCD() {
        final byte[] data =  PATTERN_M_C_D.toByteArray(EVENT);
        ByteBuffer buff = destination.getByteBuffer();
        if (buff.remaining() < data.length) {
            buff = destination.drain(buff);
        }
        buff.put(data);
        return destination.count;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long serializableMCD() {
        final String str = PATTERN_M_C_D.toSerializable(EVENT);
        final byte[] data = str.getBytes(CHARSET_DEFAULT);
        ByteBuffer buff = destination.getByteBuffer();
        if (buff.remaining() < data.length) {
            buff = destination.drain(buff);
        }
        buff.put(data);
        return destination.count;
    }

//    @Benchmark
//    @BenchmarkMode(Mode.SampleTime)
//    @OutputTimeUnit(TimeUnit.NANOSECONDS)
//    public StringBuilder toTextMCD() {
//        StringBuilder str = PATTERN_M_C_D.toText(EVENT);
//        return str;
//    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String toStringMCD() {
        final String str = PATTERN_M_C_D.toSerializable(EVENT);
        return str;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long encodeMCD() {
        PATTERN_M_C_D.encode(EVENT, destination);
        return destination.count;
    }
}

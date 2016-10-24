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
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Benchmarks the two different ways of encoding String to bytes in
 * {@link org.apache.logging.log4j.core.layout.AbstractStringLayout} for different Charsets.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar AbstractStringLayoutStringEncodingBenchmark -f 1 -i 5 -wi 5 -bm sample -tu ns
@State(Scope.Thread)
public class AbstractStringLayoutStringEncodingBenchmark {
    private static final String MESSAGE =
        "This is rather long and chatty log message with quite some interesting information and a bit of fun in it which is suitable here";

    private byte[] bytes;

    private StringLayout usAsciiGetBytesLayout;
    private StringLayout iso8859_1GetBytesLayout;
    private StringLayout utf8GetBytesLayout;
    private StringLayout utf16GetBytesLayout;

    private StringLayout usAsciiEncodeLayout;
    private StringLayout iso8859_1EncodeLayout;
    private StringLayout utf8EncodeLayout;
    private StringLayout utf16EncodeLayout;

    private LogEvent logEvent;

    private Destination destination;

    @Setup
    public void setUp() {
        bytes = new byte[128];
        for (int i = 0; i<bytes.length; i++) {
            bytes[i] = (byte)i;
        }

        usAsciiGetBytesLayout = new GetBytesLayout(Charset.forName("US-ASCII"));
        iso8859_1GetBytesLayout = new GetBytesLayout(Charset.forName("ISO-8859-1"));
        utf8GetBytesLayout = new GetBytesLayout(Charset.forName("UTF-8"));
        utf16GetBytesLayout = new GetBytesLayout(Charset.forName("UTF-16"));

        usAsciiEncodeLayout = new EncodeLayout(Charset.forName("US-ASCII"));
        iso8859_1EncodeLayout = new EncodeLayout(Charset.forName("ISO-8859-1"));
        utf8EncodeLayout = new EncodeLayout(Charset.forName("UTF-8"));
        utf16EncodeLayout = new EncodeLayout(Charset.forName("UTF-16"));

        final StringBuilder msg = new StringBuilder();
        msg.append(MESSAGE);

        logEvent = createLogEvent(new SimpleMessage(msg));

        destination = new Destination();
    }

    private static LogEvent createLogEvent(final Message message) {
        final Marker marker = null;
        final String fqcn = "com.mycom.myproject.mypackage.MyClass";
        final org.apache.logging.log4j.Level level = org.apache.logging.log4j.Level.DEBUG;
        final Throwable t = null;
        final Map<String, String> mdc = null;
        final ThreadContext.ContextStack ndc = null;
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

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void baseline() {
        consume(bytes);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void usAsciiGetBytes() {
        consume(usAsciiGetBytesLayout.toByteArray(logEvent));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void usAsciiEncode() {
        destination.reset();
        usAsciiEncodeLayout.encode(logEvent, destination);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void iso8859_1GetBytes() {
        consume(iso8859_1GetBytesLayout.toByteArray(logEvent));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void iso8859_1Encode() {
        destination.reset();
        iso8859_1EncodeLayout.encode(logEvent, destination);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void utf8GetBytes() {
        consume(utf8GetBytesLayout.toByteArray(logEvent));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void utf8Encode() {
        destination.reset();
        utf8EncodeLayout.encode(logEvent, destination);
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void utf16GetBytes() {
        consume(utf16GetBytesLayout.toByteArray(logEvent));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void utf16Encode() {
        destination.reset();
        utf16EncodeLayout.encode(logEvent, destination);
    }

    private static long consume(final byte[] bytes) {
        long checksum = 0;
        for (final byte b : bytes) {
            checksum += b;
        }
        return checksum;
    }

    private static long consume(final byte[] bytes, final int offset, final int length) {
        long checksum = 0;
        for (int i = offset; i < length; i++) {
            checksum += bytes[i];
        }
        return checksum;
    }

    private static class GetBytesLayout extends AbstractStringLayout {
        public GetBytesLayout(final Charset charset) {
            super(charset);
        }

        @Override
        public String toSerializable(final LogEvent event) {
            return null;
        }

        @Override
        public byte[] toByteArray(final LogEvent event) {
            final StringBuilder sb = getStringBuilder();
            ((StringBuilderFormattable) event.getMessage()).formatTo(sb);
            return getBytes(sb.toString());
        }
    }

    private static class EncodeLayout extends AbstractStringLayout {
        public EncodeLayout(final Charset charset) {
            super(charset);
        }

        @Override
        public String toSerializable(final LogEvent event) {
            return null;
        }

        @Override
        public byte[] toByteArray(final LogEvent event) {
            return null;
        }

        @Override
        public void encode(final LogEvent event, final ByteBufferDestination destination) {
            final StringBuilder sb = getStringBuilder();
            ((StringBuilderFormattable) event.getMessage()).formatTo(sb);
            final Encoder<StringBuilder> helper = getStringBuilderEncoder();
            helper.encode(sb, destination);
        }
    }

    private static class Destination implements ByteBufferDestination {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[512]);

        @Override
        public ByteBuffer getByteBuffer() {
            return buffer;
        }

        @Override
        public ByteBuffer drain(final ByteBuffer buf) {
            buf.flip();
            consume(buf.array(), buf.position(), buf.limit());
            buf.clear();
            return buf;
        }

        public void reset() {
            buffer.clear();
        }
    }

}

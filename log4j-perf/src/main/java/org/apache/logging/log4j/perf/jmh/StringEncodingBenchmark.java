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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Tests Log4j2 StringEncoding performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//(Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// java -jar log4j-perf/target/benchmarks.jar ".*StringEncoding.*" -f 1 -wi 5 -i 10
//
// Latency numbers instead of throughput:
// java -jar log4j-perf/target/benchmarks.jar ".*StringEncoding.*" -f 1 -wi 5 -i 10 -tu ns -bm sample
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Thread)
public class StringEncodingBenchmark {

    private final static String LOGMSG = "2015-10-02 00:26:28,517 DEBUG (main) [o.a.l.l.p.j.StringEncodingBenchmark] - Very short log message."; // length=100
    private static final String STRING_ISO8859_1 = "ISO-8859-1";
    private static final String STRING_US_ASCII = "US-ASCII";
    private static final String STRING_SHIFT_JIS = "SHIFT_JIS";
    private static final Charset CHARSET_ISO8859_1 = Charset.forName(STRING_ISO8859_1);
    private static final Charset CHARSET_DEFAULT = Charset.defaultCharset();
    private static final String DEFAULT_ENCODING = CHARSET_DEFAULT.name();
    private static final Charset CHARSET_SHIFT_JIS = Charset.forName(STRING_SHIFT_JIS);
    private static final Charset CHARSET_US_ASCII = Charset.forName(STRING_US_ASCII);
    private static final CharsetEncoder ENCODER_SHIFT_JIS = CHARSET_SHIFT_JIS.newEncoder();
    private static final CharsetEncoder ENCODER_ISO8859_1 = CHARSET_ISO8859_1.newEncoder();

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytes() {
        return LOGMSG.getBytes();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesString88591() throws Exception {
        return LOGMSG.getBytes(STRING_ISO8859_1);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesCharSet88591() {
        return LOGMSG.getBytes(CHARSET_ISO8859_1);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesStringUsAscii() throws Exception {
        return LOGMSG.getBytes(STRING_US_ASCII);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesCharSetUsAscii() {
        return LOGMSG.getBytes(CHARSET_US_ASCII);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesStringDefault() throws Exception {
        return LOGMSG.getBytes(DEFAULT_ENCODING);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesCharSetDefault() {
        return LOGMSG.getBytes(CHARSET_DEFAULT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesStringShiftJIS() throws Exception {
        return LOGMSG.getBytes(STRING_SHIFT_JIS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] stringGetBytesCharSetShiftJIS() {
        return LOGMSG.getBytes(CHARSET_SHIFT_JIS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] encoderShiftJIS() throws CharacterCodingException {
        ByteBuffer buf = ENCODER_SHIFT_JIS.encode(CharBuffer.wrap(LOGMSG));
        return buf.array();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] encoderIso8859_1() throws CharacterCodingException {
        ByteBuffer buf = ENCODER_ISO8859_1.encode(CharBuffer.wrap(LOGMSG));
        return buf.array();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] customIso8859_1() throws CharacterCodingException {
        final int length = LOGMSG.length();
        final byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) LOGMSG.charAt(i);
        }
        return result;
    }
}

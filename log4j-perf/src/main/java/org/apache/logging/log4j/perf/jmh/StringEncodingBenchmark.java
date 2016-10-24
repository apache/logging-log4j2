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
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
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
    public byte[] defaultStringGetBytes() {
        return LOGMSG.getBytes();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1StringGetBytesString() throws Exception {
        return LOGMSG.getBytes(STRING_ISO8859_1);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1StringGetBytesCharSet() {
        return LOGMSG.getBytes(CHARSET_ISO8859_1);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] usAsciiStringGetBytesString() throws Exception {
        return LOGMSG.getBytes(STRING_US_ASCII);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] usAsciiStringGetBytesCharSet() {
        return LOGMSG.getBytes(CHARSET_US_ASCII);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] defaultStringGetBytesString() throws Exception {
        return LOGMSG.getBytes(DEFAULT_ENCODING);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] defaultStringGetBytesCharSet() {
        return LOGMSG.getBytes(CHARSET_DEFAULT);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] shiftJisStringGetBytesString() throws Exception {
        return LOGMSG.getBytes(STRING_SHIFT_JIS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] shiftJisStringGetBytesCharSet() {
        return LOGMSG.getBytes(CHARSET_SHIFT_JIS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] shiftJisEncoder() throws CharacterCodingException {
        final ByteBuffer buf = ENCODER_SHIFT_JIS.encode(CharBuffer.wrap(LOGMSG));
        return buf.array();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1Encoder() throws CharacterCodingException {
        final ByteBuffer buf = ENCODER_ISO8859_1.encode(CharBuffer.wrap(LOGMSG));
        return buf.array();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1CustomCastToByte() throws CharacterCodingException {
        final int length = LOGMSG.length();
        final byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            final char c = LOGMSG.charAt(i);
            result[i++] = (byte) c;
        }
        return result;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1CustomVerifyAndCast() throws CharacterCodingException {
        final int length = LOGMSG.length();
        final byte[] result = new byte[length];
        int j = 0;
        for (int i = 0; i < length; i++) {
            final char c = LOGMSG.charAt(i);
            if (c <= 255) {
                result[j++] = (byte) c;
            } else {
                i = nonIsoChar(LOGMSG, i);
                result[j++] = (byte) '?';
            }
        }
        return result;
    }

    private int nonIsoChar(final String logmsg, int i) {
        final char c = logmsg.charAt(i++);
        if ((Character.isHighSurrogate(c)) && (i < logmsg.length()) && (Character.isLowSurrogate(logmsg.charAt(i)))) {
            i++;
        }
        return i;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1CustomPortedJDK8() throws CharacterCodingException {
        final int length = LOGMSG.length();
        final byte[] result = new byte[length];
        encode(LOGMSG, 0, length, result);
        return result;
    }

    private static int encodeISOArray(final String charArray, int charIndex, final byte[] byteArray, int byteIndex, final int length) {
        int i = 0;
        for (; i < length; i++) {
            final char c = charArray.charAt(charIndex++);
            if (c > 255) {
                break;
            }
            byteArray[(byteIndex++)] = ((byte) c);
        }
        return i;
    }

    private int encode(final String charArray, int charOffset, int charLength, final byte[] byteArray) {
        int offset = 0;
        int length = Math.min(charLength, byteArray.length);
        int charDoneIndex = charOffset + length;
        while (charOffset < charDoneIndex) {
            final int m = encodeISOArray(charArray, charOffset, byteArray, offset, length);
            charOffset += m;
            offset += m;
            if (m != length) {
                final char c = charArray.charAt(charOffset++);
                if ((Character.isHighSurrogate(c)) && (charOffset < charDoneIndex)
                        && (Character.isLowSurrogate(charArray.charAt(charOffset)))) {
                    if (charLength > byteArray.length) {
                        charDoneIndex++;
                        charLength--;
                    }
                    charOffset++;
                }
                byteArray[(offset++)] = '?';
                length = Math.min(charDoneIndex - charOffset, byteArray.length - offset);
            }
        }
        return offset;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] iso8859_1CustomPortedJDK8CopyArray() throws CharacterCodingException {
        final char[] charArray = LOGMSG.toCharArray();
        final int length = charArray.length;
        final byte[] result = new byte[length];
        encode0(charArray, 0, length, result);
        return result;
    }

    private static int encodeISOArray0(final char[] charArray, int charIndex, final byte[] byteArray, int byteIndex, final int length) {
        int i = 0;
        for (; i < length; i++) {
            final char c = charArray[(charIndex++)];
            if (c > 255) {
                break;
            }
            byteArray[(byteIndex++)] = ((byte) c);
        }
        return i;
    }

    private int encode0(final char[] charArray, int charOffset, int charLength, final byte[] byteArray) {
        int offset = 0;
        int length = Math.min(charLength, byteArray.length);
        int charDoneIndex = charOffset + length;
        while (charOffset < charDoneIndex) {
            final int m = encodeISOArray0(charArray, charOffset, byteArray, offset, length);
            charOffset += m;
            offset += m;
            if (m != length) {
                final char c = charArray[(charOffset++)];
                if ((Character.isHighSurrogate(c)) && (charOffset < charDoneIndex)
                        && (Character.isLowSurrogate(charArray[(charOffset)]))) {
                    if (charLength > byteArray.length) {
                        charDoneIndex++;
                        charLength--;
                    }
                    charOffset++;
                }
                byteArray[(offset++)] = '?';
                length = Math.min(charDoneIndex - charOffset, byteArray.length - offset);
            }
        }
        return offset;
    }
}

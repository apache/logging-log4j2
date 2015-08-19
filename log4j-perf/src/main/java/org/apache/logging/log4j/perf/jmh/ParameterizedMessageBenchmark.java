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

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ParameterizedMessage.*" -f 1 -wi 5 -i 5
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*Nanotime.*" -f 1 -wi 5 -i 5 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ParameterizedMessageBenchmark {
    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';
    private static final char ESCAPE_CHAR = '\\';
    private static final String[] ARGS = { "arg1", "arg2" };

    public static void main(final String[] args) {
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String format() {
        return ParameterizedMessage.format("pattern {} with {} two parameters and some text", ARGS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String format0() {
        return format0("pattern {} with {} two parameters and some text", ARGS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String format0_inlined2() {
        return format0_inlined2("pattern {} with {} two parameters and some text", ARGS);
    }

    public static String format0(final String messagePattern, final String[] arguments) {
        if (messagePattern == null || arguments == null || arguments.length == 0) {
            return messagePattern;
        }

        final int len = messagePattern.length();
        final char[] result = new char[len + totalLength(arguments)];
        int pos = 0;
        int escapeCounter = 0;
        int currentArgument = 0;
        for (int i = 0; i < len; i++) {
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                escapeCounter++;
            } else {
                if (curChar == DELIM_START && i < len - 1 && messagePattern.charAt(i + 1) == DELIM_STOP) {
                    // write escaped escape chars
                    int escapedEscapes = escapeCounter >> 1; // divide by 2
                    while (escapedEscapes > 0) {
                        result[pos++] = ESCAPE_CHAR;
                        escapedEscapes--;
                    }

                    if ((escapeCounter & 1) == 1) {
                        // i.e. escaped
                        // write escaped escape chars
                        result[pos++] = DELIM_START;
                        result[pos++] = DELIM_STOP;
                    } else {
                        // unescaped
                        if (currentArgument < arguments.length) {
                            final String arg = arguments[currentArgument];
                            final int argLen = arg.length();
                            arg.getChars(0, argLen, result, pos);
                            pos += argLen;
                        } else {
                            result[pos++] = DELIM_START;
                            result[pos++] = DELIM_STOP;
                        }
                        currentArgument++;
                    }
                    i++;
                    escapeCounter = 0;
                    continue;
                }
                // any other char beside ESCAPE or DELIM_START/STOP-combo
                // write unescaped escape chars
                while (escapeCounter > 0) {
                    result[pos++] = ESCAPE_CHAR;
                    escapeCounter--;
                }
                result[pos++] = curChar;
            }
        }
        return result.toString();
    }

    public static String format0_inlined2(final String messagePattern, final String[] arguments) {
        int len = 0;
        if (messagePattern == null || (len = messagePattern.length()) == 0 || arguments == null
                || arguments.length == 0) {
            return messagePattern;
        }

        return format0_inlined22(messagePattern, len, arguments);
    }

    private static String format0_inlined22(final String messagePattern, final int len, final String[] arguments) {
        final char[] result = new char[len + totalLength(arguments)];
        int pos = 0;
        int escapeCounter = 0;
        int currentArgument = 0;
        int i = 0;
        for (; i < len - 1; i++) {
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                escapeCounter++;
            } else {
                if (isDelimPair(messagePattern, i, curChar)) {
                    i++;

                    // write escaped escape chars
                    pos = format0_writeEscapedEscapeChars(escapeCounter, result, pos);

                    if (isEscapeCounterOdd(escapeCounter)) {
                        // i.e. escaped
                        // write escaped escape chars
                        pos = format0_writeDelimPair(result, pos);
                    } else {
                        // unescaped
                        pos = format0_appendArg(arguments, currentArgument, result, pos);
                        currentArgument++;
                    }
                } else {
                    pos = format0_handleLiteralChar(result, pos, escapeCounter, curChar);
                }
                escapeCounter = 0;
            }
        }
        pos = format0_handleMaybeLastChar(messagePattern, len, result, pos, escapeCounter, i);
        return new String(result, 0, pos);
    }

    // 27 bytes
    private static int totalLength(String[] arguments) {
        int result = 0;
        for (int i = 0; i < arguments.length; i++) {
            result += arguments[i].length();
        }
        return result;
    }

    // 22 bytes
    private static boolean isDelimPair(final String messagePattern, int i, final char curChar) {
        return curChar == DELIM_START && messagePattern.charAt(i + 1) == DELIM_STOP;
    }

    private static int format0_handleMaybeLastChar(final String messagePattern, final int len, final char[] result,
            int pos, int escapeCounter, int i) {
        if (i == len - 1) {
            final char curChar = messagePattern.charAt(i);
            pos = format0_handleLastChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    private static int format0_handleLastChar(final char[] result, int pos, int escapeCounter, final char curChar) {
        if (curChar == ESCAPE_CHAR) {
            pos = format0_writeUnescapedEscapeChars(escapeCounter + 1, result, pos);
        } else {
            pos = format0_handleLiteralChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    private static int format0_handleLiteralChar(final char[] result, int pos, int escapeCounter, final char curChar) {
        // any other char beside ESCAPE or DELIM_START/STOP-combo
        // write unescaped escape chars
        pos = format0_writeUnescapedEscapeChars(escapeCounter, result, pos);
        result[pos++] = curChar;
        return pos;
    }

    private static int format0_writeDelimPair(final char[] result, int pos) {
        result[pos++] = DELIM_START;
        result[pos++] = DELIM_STOP;
        return pos;
    }

    private static boolean isEscapeCounterOdd(int escapeCounter) {
        return (escapeCounter & 1) == 1;
    }

    private static int format0_writeEscapedEscapeChars(int escapeCounter, char[] result, int pos) {
        final int escapedEscapes = escapeCounter >> 1; // divide by two
        return format0_writeUnescapedEscapeChars(escapedEscapes, result, pos);
    }

    private static int format0_writeUnescapedEscapeChars(int escapeCounter, char[] result, int pos) {
        while (escapeCounter > 0) {
            result[pos++] = ESCAPE_CHAR;
            escapeCounter--;
        }
        return pos;
    }

    private static int format0_appendArg(final String[] arguments, int currentArgument, final char[] result, int pos) {
        if (currentArgument < arguments.length) {
            pos = format0_appendArg0(arguments, currentArgument, result, pos);
        } else {
            pos = format0_writeDelimPair(result, pos);
        }
        return pos;
    }

    private static int format0_appendArg0(final String[] arguments, int currentArgument, final char[] result, int pos) {
        final String arg = arguments[currentArgument];
        final int argLen = arg.length();
        arg.getChars(0, argLen, result, pos);
        return pos + argLen;
    }
}

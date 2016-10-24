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
// java -jar log4j-perf/target/benchmarks.jar ".*ParameterizedMessageInlining.*" -f 1 -wi 5 -i 10
//
// multiple threads (for example, 4 threads):
// java -jar log4j-perf/target/benchmarks.jar ".*ParameterizedMessageInlining.*" -f 1 -wi 5 -i 10 -t 4 -si true
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ParameterizedMessageInliningBenchmark {
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
        return format("pattern {} with {} two parameters and some text", ARGS);
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
    public String formatStringArgs() {
        return ParameterizedMessage.format("pattern {} with {} two parameters and some text", ARGS);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String format0_inlined2() {
        return format0_inlined2("pattern {} with {} two parameters and some text", ARGS);
    }
    public static String format(final String messagePattern, final Object[] arguments) {
        if (messagePattern == null || arguments == null || arguments.length == 0) {
            return messagePattern;
        }

        final StringBuilder result = new StringBuilder();
        int escapeCounter = 0;
        int currentArgument = 0;
        for (int i = 0; i < messagePattern.length(); i++) {
            final char curChar = messagePattern.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                escapeCounter++;
            } else {
                if (curChar == DELIM_START && i < messagePattern.length() - 1
                        && messagePattern.charAt(i + 1) == DELIM_STOP) {
                    // write escaped escape chars
                    final int escapedEscapes = escapeCounter / 2;
                    for (int j = 0; j < escapedEscapes; j++) {
                        result.append(ESCAPE_CHAR);
                    }

                    if (escapeCounter % 2 == 1) {
                        // i.e. escaped
                        // write escaped escape chars
                        result.append(DELIM_START);
                        result.append(DELIM_STOP);
                    } else {
                        // unescaped
                        if (currentArgument < arguments.length) {
                            result.append(arguments[currentArgument]);
                        } else {
                            result.append(DELIM_START).append(DELIM_STOP);
                        }
                        currentArgument++;
                    }
                    i++;
                    escapeCounter = 0;
                    continue;
                }
                // any other char beside ESCAPE or DELIM_START/STOP-combo
                // write unescaped escape chars
                if (escapeCounter > 0) {
                    for (int j = 0; j < escapeCounter; j++) {
                        result.append(ESCAPE_CHAR);
                    }
                    escapeCounter = 0;
                }
                result.append(curChar);
            }
        }
        return result.toString();
    }

    // 259 bytes
    public static String format0(final String messagePattern, final String[] arguments) {
        if (messagePattern == null || arguments == null || arguments.length == 0) {
            return messagePattern;
        }

        final int len = messagePattern.length();
        final char[] result = new char[len + sumStringLengths(arguments)];
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

    // 33 bytes
    public static String format0_inlined2(final String messagePattern, final String[] arguments) {
        int len = 0;
        if (messagePattern == null || (len = messagePattern.length()) == 0 || arguments == null
                || arguments.length == 0) {
            return messagePattern;
        }

        return format0_inlined22(messagePattern, len, arguments);
    }

    // 157 bytes
    private static String format0_inlined22(final String messagePattern, final int len, final String[] arguments) {
        final char[] result = new char[len + sumStringLengths(arguments)];
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

                    if (isOdd(escapeCounter)) {
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

    /**
     * Returns the sum of the lengths of all Strings in the specified array.
     */
    // 27 bytes
    private static int sumStringLengths(final String[] arguments) {
        int result = 0;
        for (int i = 0; i < arguments.length; i++) {
            result += arguments[i].length();
        }
        return result;
    }

    // 22 bytes
    private static boolean isDelimPair(final String messagePattern, final int i, final char curChar) {
        return curChar == DELIM_START && messagePattern.charAt(i + 1) == DELIM_STOP;
    }

    // 28 bytes
    private static int format0_handleMaybeLastChar(final String messagePattern, final int len, final char[] result,
            int pos, final int escapeCounter, final int i) {
        if (i == len - 1) {
            final char curChar = messagePattern.charAt(i);
            pos = format0_handleLastChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    // 28 bytes
    private static int format0_handleLastChar(final char[] result, int pos, final int escapeCounter, final char curChar) {
        if (curChar == ESCAPE_CHAR) {
            pos = format0_writeUnescapedEscapeChars(escapeCounter + 1, result, pos);
        } else {
            pos = format0_handleLiteralChar(result, pos, escapeCounter, curChar);
        }
        return pos;
    }

    // 16 bytes
    private static int format0_handleLiteralChar(final char[] result, int pos, final int escapeCounter, final char curChar) {
        // any other char beside ESCAPE or DELIM_START/STOP-combo
        // write unescaped escape chars
        pos = format0_writeUnescapedEscapeChars(escapeCounter, result, pos);
        result[pos++] = curChar;
        return pos;
    }

    // 18 bytes
    private static int format0_writeDelimPair(final char[] result, int pos) {
        result[pos++] = DELIM_START;
        result[pos++] = DELIM_STOP;
        return pos;
    }

    /**
     * Returns {@code true} if the specified parameter is odd.
     */
    // 11 bytes
    private static boolean isOdd(final int number) {
        return (number & 1) == 1;
    }

    // 11 bytes
    private static int format0_writeEscapedEscapeChars(final int escapeCounter, final char[] result, final int pos) {
        final int escapedEscapes = escapeCounter >> 1; // divide by two
        return format0_writeUnescapedEscapeChars(escapedEscapes, result, pos);
    }

    // 20 bytes
    private static int format0_writeUnescapedEscapeChars(int escapeCounter, final char[] result, int pos) {
        while (escapeCounter > 0) {
            result[pos++] = ESCAPE_CHAR;
            escapeCounter--;
        }
        return pos;
    }

    // 25 bytes
    private static int format0_appendArg(final String[] arguments, final int currentArgument, final char[] result, int pos) {
        if (currentArgument < arguments.length) {
            pos = format0_appendArg0(arguments, currentArgument, result, pos);
        } else {
            pos = format0_writeDelimPair(result, pos);
        }
        return pos;
    }

    // 27 bytes
    private static int format0_appendArg0(final String[] arguments, final int currentArgument, final char[] result, final int pos) {
        final String arg = arguments[currentArgument];
        final int argLen = arg.length();
        arg.getChars(0, argLen, result, pos);
        return pos + argLen;
    }
}

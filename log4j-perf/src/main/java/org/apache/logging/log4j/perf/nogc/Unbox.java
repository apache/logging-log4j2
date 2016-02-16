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
package org.apache.logging.log4j.perf.nogc;

/**
 * Unbox class proposed in LOG4J2-1278..
 */
public class Unbox {
    // TODO make thread-safe with ThreadLocals
    private static final StringBuilder[] ringbuffer;
    private static int current;
    private static final int MASK = 16 - 1;

    static {
        ringbuffer = new StringBuilder[16];
        for (int i = 0; i < ringbuffer.length; i++) {
            ringbuffer[i] = new StringBuilder();
        }
    }

    public static StringBuilder box(float value) {
        return getSB().append(value);
    }

    public static StringBuilder box(double value) {
        return getSB().append(value);
    }

    public static StringBuilder box(short value) {
        return getSB().append(value);
    }

    public static StringBuilder box(int value) {
        return getSB().append(value);
    }

    public static StringBuilder box(long value) {
        return getSB().append(value);
    }

    private static StringBuilder getSB() {
        StringBuilder result = ringbuffer[MASK & current++];
        result.setLength(0);
        return result;
    }
}

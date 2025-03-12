/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.CoreProperties.UuidProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;

/**
 * Generates a unique ID. The generated UUID will be unique for approximately 8,925 years so long as
 * less than 10,000 IDs are generated per millisecond on the same device (as identified by its MAC address).
 */
public final class UuidUtil {

    private static final String ASSIGNED_SEQUENCES = "org.apache.logging.log4j.assignedSequences";

    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final long TYPE1 = 0x1000L;
    private static final byte VARIANT = (byte) 0x80;
    private static final int SEQUENCE_MASK = 0x3FFF;
    private static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
    private static final long INITIAL_UUID_SEQNO =
            PropertyEnvironment.getGlobal().getProperty(UuidProperties.class).sequence();

    private static final long LOW_MASK = 0xffffffffL;
    private static final long MID_MASK = 0xffff00000000L;
    private static final long HIGH_MASK = 0xfff000000000000L;
    private static final int NODE_SIZE = 8;
    private static final int SHIFT_2 = 16;
    private static final int SHIFT_4 = 32;
    private static final int SHIFT_6 = 48;
    private static final int HUNDRED_NANOS_PER_MILLI = 10000;

    private static final long LEAST = initialize(NetUtils.getMacAddress());
    private static final long SALT = new SecureRandom().nextLong();

    /* This class cannot be instantiated */
    private UuidUtil() {}

    /**
     * Initializes this class
     *
     * @param mac MAC address
     * @return Least
     */
    static long initialize(byte[] mac) {
        final Random randomGenerator = new SecureRandom();
        if (mac == null || mac.length == 0) {
            mac = new byte[6];
            randomGenerator.nextBytes(mac);
        }
        final int length = Math.min(mac.length, 6);
        final int index = mac.length >= 6 ? mac.length - 6 : 0;
        final byte[] node = new byte[NODE_SIZE];
        node[0] = VARIANT;
        node[1] = 0;
        for (int i = 2; i < NODE_SIZE; ++i) {
            node[i] = 0;
        }
        System.arraycopy(mac, index, node, 2, length);
        final ByteBuffer buf = ByteBuffer.wrap(node);
        long rand = INITIAL_UUID_SEQNO;
        String assigned = System.getProperty(ASSIGNED_SEQUENCES);
        final long[] sequences;
        if (assigned == null) {
            sequences = new long[0];
        } else {
            final String[] array = assigned.split(Patterns.COMMA_SEPARATOR);
            sequences = new long[array.length];
            int i = 0;
            for (final String value : array) {
                sequences[i] = Long.parseLong(value);
                ++i;
            }
        }
        if (rand == 0) {
            rand = randomGenerator.nextLong();
        }
        rand &= SEQUENCE_MASK;
        boolean duplicate;
        do {
            duplicate = false;
            for (final long sequence : sequences) {
                if (sequence == rand) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                rand = (rand + 1) & SEQUENCE_MASK;
            }
        } while (duplicate);
        assigned = assigned == null ? Long.toString(rand) : assigned + ',' + Long.toString(rand);
        System.setProperty(ASSIGNED_SEQUENCES, assigned);

        return buf.getLong() | rand << SHIFT_6;
    }

    /**
     * Generates Type 1 UUID. The time contains the number of 100NS intervals that have occurred
     * since 00:00:00.00 UTC, 10 October 1582. Each UUID on a particular machine is unique to the 100NS interval
     * until they rollover around 3400 A.D.
     * <ol>
     * <li>Digits 1-12 are the lower 48 bits of the number of 100 ns increments since the start of the UUID
     * epoch.</li>
     * <li>Digit 13 is the version (with a value of 1).</li>
     * <li>Digits 14-16 are a sequence number that is incremented each time a UUID is generated.</li>
     * <li>Digit 17 is the variant (with a value of binary 10) and 10 bits of the sequence number</li>
     * <li>Digit 18 is final 16 bits of the sequence number.</li>
     * <li>Digits 19-32 represent the system the application is running on.</li>
     * </ol>
     *
     * @return universally unique identifiers (UUID)
     */
    public static UUID getTimeBasedUuid() {

        final long time =
                ((System.currentTimeMillis() * HUNDRED_NANOS_PER_MILLI) + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH)
                        + (COUNT.incrementAndGet() % HUNDRED_NANOS_PER_MILLI);
        final long timeLow = (time & LOW_MASK) << SHIFT_4;
        final long timeMid = (time & MID_MASK) >> SHIFT_2;
        final long timeHi = (time & HIGH_MASK) >> SHIFT_6;
        final long most = timeLow | timeMid | TYPE1 | timeHi;
        return new UUID(most, LEAST);
    }

    /**
     * Generates a Type 4 UUID based on the deterministic LogEvent hash.
     * Meant for generating consistent, correlatable UUID values across multiple Appenders for the same LogEvent.
     *
     * @param logEvent
     * @return universally unique identifiers (UUID)
     */
    public static UUID getLogEventBasedUuid(LogEvent logEvent) {
        // TODO: better hashing algorithm - include other LogEvent fields?
        long epochSecond = logEvent.getInstant().getEpochSecond();
        // Enable 'log4j.configuration.usePreciseClock' system property otherwise will be truncated to millis
        long nanoOfSecond = logEvent.getInstant().getNanoOfSecond();
        // Thread IDs typically increment from 0 producing a narrow range
        long threadId = logEvent.getThreadId();

        // Increase entropy
        long most = mix(epochSecond, nanoOfSecond, threadId);
        long least = mix(threadId, ~nanoOfSecond, epochSecond);
        // Set UUID v4 bits
        most &= 0xFFFFFFFFFFFF0FFFL;
        most |= 0x0000000000004000L;
        least &= 0x3FFFFFFFFFFFFFFFL;
        least |= 0x8000000000000000L;

        return new UUID(most, least);
    }

    private static long mix(long v1, long v2, long v3) {
        // XOR with large primes
        long hash = v1 * 0x9E3779B97F4A7C15L;
        hash ^= (v2 * 0xC6BC279692B5C323L);
        hash ^= (v3 * 0x3243F6A8885A308DL);
        // Scramble
        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);
        // Add salt
        hash ^= SALT;
        return hash;
    }
}

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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Generates a unique ID. The generated UUID will be unique for approximately 8,925 years so long as
 * less than 10,000 IDs are generated per millisecond on the same device (as identified by its MAC address).
 */
public final class UuidUtil {

    private static final long[] EMPTY_LONG_ARRAY = {};

    /**
     * System property that may be used to seed the UUID generation with an integer value.
     */
    public static final String UUID_SEQUENCE = "org.apache.logging.log4j.uuidSequence";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String ASSIGNED_SEQUENCES = "org.apache.logging.log4j.assignedSequences";

    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final long TYPE1 = 0x1000L;
    private static final byte VARIANT = (byte) 0x80;
    private static final int SEQUENCE_MASK = 0x3FFF;
    private static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
    private static final long INITIAL_UUID_SEQNO =
            PropertiesUtil.getProperties().getLongProperty(UUID_SEQUENCE, 0);

    private static final long LOW_MASK = 0xffffffffL;
    private static final long MID_MASK = 0xffff00000000L;
    private static final long HIGH_MASK = 0xfff000000000000L;
    private static final int NODE_SIZE = 8;
    private static final int SHIFT_2 = 16;
    private static final int SHIFT_4 = 32;
    private static final int SHIFT_6 = 48;
    private static final int HUNDRED_NANOS_PER_MILLI = 10000;

    private static final long LEAST = initialize(NetUtils.getMacAddress());

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
        final int length = mac.length >= 6 ? 6 : mac.length;
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
        String assigned = PropertiesUtil.getProperties().getStringProperty(ASSIGNED_SEQUENCES);
        long[] sequences;
        if (assigned == null) {
            sequences = EMPTY_LONG_ARRAY;
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
}

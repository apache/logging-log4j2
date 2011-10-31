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
package org.apache.logging.log4j.core.appender.flume;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a unique id. The generated UUID will be unique for approximately 8,925 years so long as
 * less than 4095 ids are generated per millisecond on the same device (as identified by its MAC adddress).
 */
public abstract class UUIDUtil
{
    private static AtomicInteger count = new AtomicInteger(0);

    private static final long VERSION = 0x9000L;

    private static final long TYPE1 = 0x1000L;

    private static final byte VARIANT = (byte)0x80;

    private static final int SEQUENCE_MASK = 0x3FFF;

    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

    private static long least;


    static
    {
        byte[] mac = null;
        try
        {
            InetAddress address = InetAddress.getLocalHost();

            try {
                NetworkInterface ni = NetworkInterface.getByInetAddress(address);
                if (!ni.isLoopback() && ni.isUp()) {
                    Method method = ni.getClass().getMethod("getHardwareAddress");
                    mac = (byte[]) method.invoke(ni);
                }
                else {
                    Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
                    while (enumeration.hasMoreElements() && mac == null) {
                        ni = enumeration.nextElement();
                        if (ni.isUp() && !ni.isLoopback()) {
                            Method method = ni.getClass().getMethod("getHardwareAddress");
                            mac = (byte[]) method.invoke(ni);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // Ignore exception
            }
            if (mac == null || mac.length == 0) {
                mac = address.getAddress();
            }
        }
        catch (UnknownHostException e) {
            // Ignore exception
        }
        Random randomGenerator = new SecureRandom();
        if (mac == null || mac.length == 0) {
            mac = new byte[6];
            randomGenerator.nextBytes(mac);
        }
        int length = mac.length >= 6 ? 6 : mac.length;
        int index = mac.length >= 6 ? mac.length - 6 : 0;
        byte[] node = new byte[8];
        node[0] = VARIANT;
        node[1] = 0;
        for (int i=2; i < 8 ; ++i) {
            node[i] = 0;
        }
        System.arraycopy(mac, index, node, index + 2, length);
        ByteBuffer buf = ByteBuffer.wrap(node);
        long rand = (randomGenerator.nextLong() & SEQUENCE_MASK) << 48;
        least = buf.getLong() | rand;
    }

    private static String toHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;

        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v/16];
            hexChars[j*2 + 1] = hexArray[v%16];
        }
        return new String(hexChars);
    }

    /* This class cannot be instantiated */
    private UUIDUtil() {
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
     * <li>Digits 19-32 represent the system the application is running on.
     * </ol>
     *
     * @return universally unique identifiers (UUID)
     */
    public static UUID getTimeBasedUUID() {

        long time = ((System.currentTimeMillis() * 10000) + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) +
            (count.incrementAndGet() % 10000);
        long timeLow = (time & 0xffffffffL) << 32;
        long timeMid = (time & 0xffff00000000L) >> 16;
        long timeHi = (time & 0xfff000000000000L) >> 48;
        long most = timeLow | timeMid | TYPE1 | timeHi;
        return new UUID(most, least);
    }
}


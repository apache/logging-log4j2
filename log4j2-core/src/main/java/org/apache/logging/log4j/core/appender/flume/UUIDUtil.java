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

    private static final byte VARIANT = (byte)0xC0;

    private static long least;

    static
    {
        byte[] mac = null;
        try
        {
            InetAddress address = InetAddress.getLocalHost();

            try {
                NetworkInterface ni = NetworkInterface.getByInetAddress(address);
                if (ni != null) {
                    Method method = ni.getClass().getMethod("getHardwareAddress");
                    mac = (byte[]) method.invoke(ni);
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
        if (mac == null || mac.length == 0) {
            Random randomGenerator = new SecureRandom();
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
        least = buf.getLong();
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
     * Convert a UUID to a String with no dashes.
     * @param uuid The UUID.
     * @return The String version of the UUID with the '-' characters removed.
     */
    public static String getUUIDString(UUID uuid)
    {
        return StringUtils.replaceChars(uuid.toString(), "-", "");
    }

    /**
     * Generates universally unique identifiers (UUIDs).
     * UUID combines enough of the system information to make it unique across
     * space and time. UUID string is composed of following fields:
     * <ol>
     * <li>Digits 1-12 are the lower 48 bits of the <code>System.currentTimeMillis()</code> call.
     * This makes the UUID unique down to the millisecond for about 8,925 years.</li>
     * <li>Digit 13 is the version (with a value of 9).</li>
     * <li>Digits 14-16 are a sequence number that is incremented each time a UUID is generated.</li>
     * <li>Digit 17 is the variant (with a value of 0xC)</li>
     * <li>Digit 18 is zero.</li>
     * <li>Digits 19-32 represent the system the application is running on.
     * </ol>
     *
     * @return universally unique identifiers (UUID)
     */
    public static UUID getTimeBasedUUID()
    {
        int timeHi = count.incrementAndGet() & 0xfff;
        long most = (System.currentTimeMillis() << 24) | VERSION | timeHi;

        return new UUID(most, least);
    }
}


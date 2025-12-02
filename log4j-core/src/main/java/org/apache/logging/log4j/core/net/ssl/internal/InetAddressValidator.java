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
package org.apache.logging.log4j.core.net.ssl.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;

/**
 * {@link java.net.InetAddress} literal validator.
 *
 * @implNote
 * IP address validation is hard.
 * IPv6 validation is copied from Apache Commons Validator.
 * This is an internal class with a very limited usage, and should stay that way.
 * This class should be replaced with {@code java.net.InetAddress#ofLiteral(String)} introduced in Java 22.
 *
 * @see <a href="https://github.com/apache/commons-validator/blob/7c27355d86f7dc5a4a548658745b85c9f0d5b99f/src/main/java/org/apache/commons/validator/routines/InetAddressValidator.java"><code>InetAddressValidator</code> of Apache Commons Validator</a>
 */
public final class InetAddressValidator {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    /** Max number of hex groups (separated by {@code :}) in an IPV6 address */
    private static final int IPV6_MAX_HEX_GROUPS = 8;

    /** Max hex digits in each IPv6 group */
    private static final int IPV6_MAX_HEX_DIGITS_PER_GROUP = 4;

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d{1,3}");

    private static final Pattern ID_CHECK_PATTERN = Pattern.compile("[^\\s/%]+");

    private InetAddressValidator() {}

    /**
     * {@return {@code true} if the provided string is a valid IPv4 or IPv6 address, {@code false} otherwise}.
     * Verification is performed without any name resolution.
     *
     * @param inetAddress the string to validate
     */
    public static boolean isValid(final String inetAddress) {
        return isValidInet4Address(inetAddress) || isValidInet6Address(inetAddress);
    }

    static boolean isValidInet4Address(final String inet4Address) {
        final Matcher m = IPV4_PATTERN.matcher(inet4Address);
        if (!m.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            final String g = m.group(i);
            if (g.length() > 1 && g.startsWith("0")) {
                return false;
            }
            final int n;
            try {
                n = Integer.parseInt(g);
            } catch (final NumberFormatException ignored) {
                return false;
            }
            if (n > 0xFF) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidInet6Address(String inet6Address) {
        String[] parts;
        // remove prefix size. This will appear after the zone id (if any)
        parts = inet6Address.split("/", -1);
        if (parts.length > 2) {
            return false; // can only have one prefix specifier
        }
        if (parts.length == 2) {
            if (!DIGITS_PATTERN.matcher(parts[1]).matches()) {
                return false; // not a valid number
            }
            final int bits = Integer.parseInt(parts[1]); // cannot fail because of RE check
            if (bits < 0 || bits > 128) {
                return false; // out of range
            }
        }
        // remove zone-id
        parts = parts[0].split("%", -1);
        // The id syntax is implementation independent, but it presumably cannot allow:
        // whitespace, '/' or '%'
        if (parts.length > 2
                || parts.length == 2 && !ID_CHECK_PATTERN.matcher(parts[1]).matches()) {
            return false; // invalid id
        }
        inet6Address = parts[0];
        final boolean containsCompressedZeroes = inet6Address.contains("::");
        if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
            return false;
        }
        final boolean startsWithCompressed = inet6Address.startsWith("::");
        final boolean endsWithCompressed = inet6Address.endsWith("::");
        final boolean endsWithSep = inet6Address.endsWith(":");
        if (inet6Address.startsWith(":") && !startsWithCompressed || endsWithSep && !endsWithCompressed) {
            return false;
        }
        String[] octets = inet6Address.split(":");
        if (containsCompressedZeroes) {
            final List<String> octetList = new ArrayList<>(Arrays.asList(octets));
            if (endsWithCompressed) {
                // String.split() drops ending empty segments
                octetList.add("");
            } else if (startsWithCompressed && !octetList.isEmpty()) {
                octetList.remove(0);
            }
            octets = octetList.toArray(new String[0]);
        }
        if (octets.length > IPV6_MAX_HEX_GROUPS) {
            return false;
        }
        int validOctets = 0;
        int emptyOctets = 0; // consecutive empty chunks
        for (int index = 0; index < octets.length; index++) {
            final String octet = octets[index];
            if (Strings.isBlank(octet)) {
                emptyOctets++;
                if (emptyOctets > 1) {
                    return false;
                }
            } else {
                emptyOctets = 0;
                // Is last chunk an IPv4 address?
                if (index == octets.length - 1 && octet.contains(".")) {
                    if (!isValidInet4Address(octet)) {
                        return false;
                    }
                    validOctets += 2;
                    continue;
                }
                if (octet.length() > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
                    return false;
                }
                int octetInt = 0;
                try {
                    octetInt = Integer.parseInt(octet, 16);
                } catch (final NumberFormatException e) {
                    return false;
                }
                if (octetInt < 0 || octetInt > 0xffff) {
                    return false;
                }
            }
            validOctets++;
        }
        return validOctets <= IPV6_MAX_HEX_GROUPS && (validOctets >= IPV6_MAX_HEX_GROUPS || containsCompressedZeroes);
    }
}

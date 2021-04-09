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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.util.Strings;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 *
 */
public final class NameUtil {

    private NameUtil() {}

    public static String getSubName(final String name) {
        if (Strings.isEmpty(name)) {
            return null;
        }
        final int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : Strings.EMPTY;
    }

    /**
     * Calculates the <a href="https://en.wikipedia.org/wiki/MD5">MD5</a> hash
     * of the given input string encoded using the default platform
     * {@link Charset charset}.
     * <p>
     * <b>MD5 has severe vulnerabilities and should not be used for sharing any
     * sensitive information.</b> This function should only be used to create
     * unique identifiers, e.g., configuration element names.
     *
     * @param input string to be hashed
     * @return string composed of 32 hexadecimal digits of the calculated hash
     */
    public static String md5(final String input) {
        Objects.requireNonNull(input, "input");
        try {
            final byte[] inputBytes = input.getBytes();
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final byte[] bytes = digest.digest(inputBytes);
            final StringBuilder md5 = new StringBuilder(bytes.length * 2);
            for (final byte b : bytes) {
                final String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    md5.append('0');
                }
                md5.append(hex);
            }
            return md5.toString();
        }
        // Every implementation of the Java platform is required to support MD5.
        // Hence, this catch block should be unreachable.
        // See https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html
        // for details.
        catch (final NoSuchAlgorithmException error) {
            throw new RuntimeException(error);
        }
    }

}

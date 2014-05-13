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

import java.security.MessageDigest;

import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public final class NameUtil {

    private static final int MASK = 0xff;

    private NameUtil() {
    }

    public static String getSubName(final String name) {
        if (name.isEmpty()) {
            return null;
        }
        final int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : Strings.EMPTY;
    }

    public static String md5(final String string) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(string.getBytes());
            final byte[] bytes = digest.digest();
            final StringBuilder md5 = new StringBuilder();
            for (final byte b : bytes) {
                final String hex = Integer.toHexString(MASK & b);
                if (hex.length() == 1) {
                    md5.append('0');
                }
                md5.append(hex);
            }
            return md5.toString();
        } catch (final Exception ex) {
            return string;
        }
    }
}

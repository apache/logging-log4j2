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

import java.nio.charset.Charset;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Charset utilities. Contains the standard character sets guaranteed to be available on all implementations of the
 * Java platform. Parts adapted from JDK 1.7 (in particular, the {@code java.nio.charset.StandardCharsets} class).
 *
 * @see java.nio.charset.Charset
 */
public final class Charsets {

    /**
     * Seven-bit ASCII. ISO646-US. The Basic Latin block of the Unicode character set.
     */
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
     */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     * Eight-bit UCS Transformation Format.
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order.
     */
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");

    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order.
     */
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark.
     */
    public static final Charset UTF_16 = Charset.forName("UTF-16");

    /**
     * Returns a Charset, if possible the Charset for the specified {@code charsetName}, otherwise (if the specified
     * {@code charsetName} is {@code null} or not supported) this method returns the platform default Charset.
     *
     * @param charsetName
     *            name of the preferred charset or {@code null}
     * @return a Charset, not null.
     */
    public static Charset getSupportedCharset(final String charsetName) {
        return getSupportedCharset(charsetName, Charset.defaultCharset());
    }

    /**
     * Returns a Charset, if possible the Charset for the specified {@code charsetName}, otherwise (if the specified
     * {@code charsetName} is {@code null} or not supported) this method returns the platform default Charset.
     *
     * @param charsetName
     *            name of the preferred charset or {@code null}
     * @param defaultCharset
     *            returned if {@code charsetName} is null or is not supported.
     * @return a Charset, never null.
     */
    public static Charset getSupportedCharset(final String charsetName, final Charset defaultCharset) {
        Charset charset = null;
        if (charsetName != null && Charset.isSupported(charsetName)) {
            charset = Charset.forName(charsetName);
        }
        if (charset == null) {
            charset = defaultCharset;
            if (charsetName != null) {
                StatusLogger.getLogger().error(
                        "Charset " + charsetName + " is not supported for layout, using " + charset.displayName());
            }
        }
        return charset;
    }

    private Charsets() {
    }

}

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
package org.apache.logging.log4j.core.helpers;

import java.nio.charset.Charset;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Charset utilities.
 */
public final class Charsets {

    /**
     * UTF-8 Charset.
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

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
        if (charsetName != null) {
            if (Charset.isSupported(charsetName)) {
                charset = Charset.forName(charsetName);
            }
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

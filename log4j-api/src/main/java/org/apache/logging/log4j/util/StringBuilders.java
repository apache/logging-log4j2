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
package org.apache.logging.log4j.util;

import java.util.Map.Entry;

/**
 * <em>Consider this class private.</em>
 */
public final class StringBuilders {
    private StringBuilders() {
    }

    /**
     * Appends in the following format: double quoted value.
     * 
     * @param sb a string builder
     * @param value a value
     * @return {@code "value"}
     */
    public static StringBuilder appendDqValue(final StringBuilder sb, final Object value) {
        return sb.append(Chars.DQUOTE).append(value).append(Chars.DQUOTE);
    }

    /**
     * Appends in the following format: key=double quoted value.
     * 
     * @param sb a string builder
     * @param entry a map entry
     * @return {@code key="value"}
     */
    public static StringBuilder appendKeyDqValue(final StringBuilder sb, final Entry<String, String> entry) {
        return appendKeyDqValue(sb, entry.getKey(), entry.getValue());
    }

    /**
     * Appends in the following format: key=double quoted value.
     * 
     * @param sb a string builder
     * @param key a key
     * @param value a value
     * @return the specified StringBuilder
     */
    public static StringBuilder appendKeyDqValue(final StringBuilder sb, final String key, final Object value) {
        return sb.append(key).append(Chars.EQ).append(Chars.DQUOTE).append(value).append(Chars.DQUOTE);
    }

}

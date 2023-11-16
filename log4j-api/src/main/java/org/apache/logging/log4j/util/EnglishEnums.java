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
package org.apache.logging.log4j.util;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

/**
 * <em>Consider this class private.</em>
 *
 * <p>
 * Helps convert English Strings to English Enum values.
 * </p>
 * <p>
 * Enum name arguments are converted internally to upper case with the {@linkplain java.util.Locale#ENGLISH ENGLISH} locale to
 * avoid problems on the Turkish locale. Do not use with Turkish enum values.
 * </p>
 */
@InternalApi
public final class EnglishEnums {

    private EnglishEnums() {}

    /**
     * Returns the Result for the given string.
     * <p>
     * The {@code name} is converted internally to upper case with the {@linkplain java.util.Locale#ENGLISH ENGLISH} locale to
     * avoid problems on the Turkish locale. Do not use with Turkish enum values.
     * </p>
     *
     * @param enumType The Class of the enum.
     * @param name The enum name, case-insensitive. If null, returns {@code defaultValue}.
     * @param <T> The type of the enum.
     * @return an enum value or null if {@code name} is null.
     */
    public static <T extends Enum<T>> T valueOf(final Class<T> enumType, final String name) {
        return valueOf(enumType, name, null);
    }

    /**
     * Returns an enum value for the given string.
     * <p>
     * The {@code name} is converted internally to upper case with the {@linkplain java.util.Locale#ENGLISH ENGLISH} locale to
     * avoid problems on the Turkish locale. Do not use with Turkish enum values.
     * </p>
     *
     * @param name The enum name, case-insensitive. If null, returns {@code defaultValue}.
     * @param enumType The Class of the enum.
     * @param defaultValue the enum value to return if {@code name} is null.
     * @param <T> The type of the enum.
     * @return an enum value or {@code defaultValue} if {@code name} is null.
     */
    public static <T extends Enum<T>> T valueOf(final Class<T> enumType, final String name, final T defaultValue) {
        return name == null ? defaultValue : Enum.valueOf(enumType, toRootUpperCase(name));
    }
}

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
package org.apache.logging.log4j.core.config.plugins.convert;

import org.apache.logging.log4j.util.EnglishEnums;

/**
 * Converts a {@link String} into a {@link Enum}. Returns {@code null} for invalid enum names.
 *
 * @param <E> the enum class to parse.
 * @since 2.1 moved from TypeConverters
 */
public class EnumConverter<E extends Enum<E>> implements TypeConverter<E> {
    private final Class<E> clazz;

    public EnumConverter(final Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public E convert(final String s) {
        return EnglishEnums.valueOf(clazz, s);
    }
}

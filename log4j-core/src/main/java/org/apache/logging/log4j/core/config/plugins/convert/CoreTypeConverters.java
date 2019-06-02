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

package org.apache.logging.log4j.core.config.plugins.convert;

import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverters;

/**
 * Core specific type converters.
 *
 * @since 2.1 Moved to the {@code convert} package.
 */
public final class CoreTypeConverters {

    @Plugin(name = "CronExpression", category = TypeConverters.CATEGORY)
    public static class CronExpressionConverter implements TypeConverter<CronExpression> {
        @Override
        public CronExpression convert(final String s) throws Exception {
            return new CronExpression(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Duration}.
     * @since 2.5
     */
    @Plugin(name = "Duration", category = TypeConverters.CATEGORY)
    public static class DurationConverter implements TypeConverter<Duration> {
        @Override
        public Duration convert(final String s) {
            return Duration.parse(s);
        }
    }
}

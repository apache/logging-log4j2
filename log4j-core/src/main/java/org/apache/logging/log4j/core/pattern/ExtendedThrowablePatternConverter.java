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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link ThrowablePatternConverter} variant where the rendered {@link StackTraceElement}s are enriched with the enclosing JAR file and its version information, if available.
 */
@NullMarked
@Plugin(name = "ExtendedThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"xEx", "xThrowable", "xException"})
public final class ExtendedThrowablePatternConverter extends ThrowablePatternConverter {

    private ExtendedThrowablePatternConverter(@Nullable final Configuration config, @Nullable final String[] options) {
        super(
                "ExtendedThrowable",
                "throwable",
                options,
                config,
                ThrowablePropertyRendererFactory.INSTANCE,
                ThrowableExtendedStackTraceRendererFactory.INSTANCE);
    }

    private static ThrowableExtendedStackTraceRenderer createRenderer(final ThrowableFormatOptions options) {
        return new ThrowableExtendedStackTraceRenderer(options.getIgnorePackages(), options.getLines());
    }

    /**
     * Creates an instance of the class.
     *
     * @param config a configuration
     * @param options the pattern options
     * @return a new instance
     */
    public static ExtendedThrowablePatternConverter newInstance(
            @Nullable final Configuration config, @Nullable final String[] options) {
        return new ExtendedThrowablePatternConverter(config, options);
    }
}

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
package org.apache.logging.log4j.core.pattern;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Abstract base class for other pattern converters which can return only parts of their name.
 */
@PerformanceSensitive("allocation")
public abstract class NamePatternConverter extends LogEventPatternConverter {
    /**
     * Abbreviator.
     */
    private final NameAbbreviator abbreviator;

    /**
     * Optional cache of previously encountered names i.e. logger and class names (keys) and conversion results (values).
     */
    private final Map<String, String> conversionCache = createConversionCache();

    /**
     * Constructor.
     *
     * @param name    name of converter.
     * @param style   style name for associated output.
     * @param options options, may be null, first element will be interpreted as an abbreviation pattern.
     */
    protected NamePatternConverter(final String name, final String style, final String[] options) {
        super(name, style);

        if (options != null && options.length > 0) {
            abbreviator = NameAbbreviator.getAbbreviator(options[0]);
        } else {
            abbreviator = NameAbbreviator.getDefaultAbbreviator();
        }
    }

    /**
     * Abbreviate name in string buffer, with optional caching of results.
     *
     * @param original string containing name.
     * @param destination the StringBuilder to write to
     * @return The abbreviated name.
     */
    protected final void abbreviate(final String original, final StringBuilder destination) {
        if (conversionCache != null) {
            // with caching enabled, the number of abbreviations to be computed is reduced
            String abbr = conversionCache.computeIfAbsent(original, orig -> {
                abbreviator.abbreviate(orig, destination);
                return destination.toString();
            });
            destination.setLength(0);
            destination.append(abbr);
        } else {
            abbreviator.abbreviate(original, destination);
        }
    }

    /**
     * Creates the optional conversion cache if system property {@link Constants#LOG4J_NAME_PATTERN_CONVERSION_CACHE_SIZE}
     * is an integer greater than zero. The cache is limited to the configured size and defaults to <code>250</code>.<br>
     * Once the size is reached, a new entry is added and the oldest entry removed.
     * @return cache (a synchronized bounded map)
     */
    private Map<String, String> createConversionCache() {
        final int defCacheSize = 250;
        final Integer cacheSize = Integer.getInteger(Constants.LOG4J_NAME_PATTERN_CONVERSION_CACHE_SIZE, defCacheSize);
        return cacheSize > 0 ? Collections.synchronizedMap(new BoundedLinkedHashMap(cacheSize)) : null;
    }

    private static final class BoundedLinkedHashMap extends LinkedHashMap<String, String> {
        private final int maxSize;

        private BoundedLinkedHashMap(int maxSize) {
            super(maxSize); // argument is validated in super constructor
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> _eldest) {
            return size() > maxSize;
        }
    }

}

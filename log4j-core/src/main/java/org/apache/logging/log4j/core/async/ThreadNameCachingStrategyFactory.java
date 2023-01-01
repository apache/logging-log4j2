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
package org.apache.logging.log4j.core.async;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.plugins.ContextScoped;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertyResolver;

@ContextScoped
public class ThreadNameCachingStrategyFactory implements Supplier<ThreadNameCachingStrategy> {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    static final ThreadNameCachingStrategy DEFAULT_STRATEGY =
            isAllocatingThreadGetName() ? ThreadNameCachingStrategy.CACHED : ThreadNameCachingStrategy.UNCACHED;
    private final PropertyResolver propertyResolver;

    @Inject
    public ThreadNameCachingStrategyFactory(final PropertyResolver resolver) {
        propertyResolver = resolver;
    }

    @Override
    public ThreadNameCachingStrategy get() {
        return propertyResolver
                .getString(Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY)
                .map(name -> {
                    try {
                        final ThreadNameCachingStrategy result = ThreadNameCachingStrategy.valueOf(name);
                        LOGGER.debug("{}={} (user specified {}, default is {})",
                                Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY, result.name(), name,
                                DEFAULT_STRATEGY.name());
                        return result;
                    } catch (final Exception e) {
                        LOGGER.debug("Using {}.{}: '{}' not valid",
                                Log4jProperties.ASYNC_LOGGER_THREAD_NAME_STRATEGY, DEFAULT_STRATEGY.name(), name, e);
                        return DEFAULT_STRATEGY;
                    }
                })
                .orElse(DEFAULT_STRATEGY);
    }

    static boolean isAllocatingThreadGetName() {
        // LOG4J2-2052, LOG4J2-2635 JDK 8u102 ("1.8.0_102") removed the String allocation in Thread.getName()
        if (Constants.JAVA_MAJOR_VERSION == 8) {
            try {
                final Pattern javaVersionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)");
                final Matcher m = javaVersionPattern.matcher(System.getProperty("java.version"));
                if (m.matches()) {
                    return Integer.parseInt(m.group(3)) == 0 && Integer.parseInt(m.group(4)) < 102;
                }
                return true;
            } catch (final Exception e) {
                return true;
            }
        } else {
            return Constants.JAVA_MAJOR_VERSION < 8;
        }
    }
}

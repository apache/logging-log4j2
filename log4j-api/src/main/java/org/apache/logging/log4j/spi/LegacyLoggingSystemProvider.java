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

package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util3.Cast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Properties;

/**
 * Logging system provider for wrapping legacy Log4j providers specified via properties files.
 */
public class LegacyLoggingSystemProvider<F extends LoggerContextFactory> extends AbstractLoggingSystemProvider<F> {
    /**
     * Property name to set for a Log4j 2 provider to specify the priority of this implementation.
     */
    public static final String FACTORY_PRIORITY = "FactoryPriority";
    /**
     * Property name to set to the implementation of {@link org.apache.logging.log4j.spi.ThreadContextMap}.
     */
    public static final String THREAD_CONTEXT_MAP = "ThreadContextMap";
    /**
     * Property name to set to the implementation of {@link org.apache.logging.log4j.spi.LoggerContextFactory}.
     */
    public static final String LOGGER_CONTEXT_FACTORY = "LoggerContextFactory";
    private static final String API_VERSION = "Log4jAPIVersion";

    private final Logger logger = StatusLogger.getLogger();
    private final URL url;
    private final WeakReference<ClassLoader> classLoaderRef;
    private final int priority;
    private final String version;
    private final String className;
    private final String threadContextMap;

    public LegacyLoggingSystemProvider(final URL url, final ClassLoader classLoader) throws IOException {
        this(loadFromUrl(url), url, classLoader);
    }

    public LegacyLoggingSystemProvider(final Properties props, final URL url, final ClassLoader classLoader) {
        this.url = url;
        this.classLoaderRef = new WeakReference<>(classLoader);
        final String weight = props.getProperty(FACTORY_PRIORITY);
        priority = weight == null ? -1 : Integer.parseInt(weight);
        version = props.getProperty(API_VERSION);
        className = props.getProperty(LOGGER_CONTEXT_FACTORY);
        threadContextMap = props.getProperty(THREAD_CONTEXT_MAP);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    protected F createLoggerContextFactory() {
        if (className == null) {
            logger.error("No class name defined for Log4j provider at {}", url);
            return null;
        }
        final ClassLoader classLoader = classLoaderRef.get();
        if (classLoader == null) {
            logger.error("No ClassLoader defined for Log4j provider at {}", url);
            return null;
        }
        try {
            final Class<?> clazz = classLoader.loadClass(className);
            final Class<F> factoryClass = Cast.cast(clazz.asSubclass(LoggerContextFactory.class));
            return getInstance(factoryClass);
        } catch (final Exception e) {
            logger.error("Unable to create class {} specified in {}", className, url, e);
        }
        return null;
    }

    @Override
    protected ThreadContextMap.Factory createContextMapFactory() {
        if (threadContextMap != null) {
            final ClassLoader classLoader = classLoaderRef.get();
            if (classLoader != null) {
                try {
                    final Class<?> mapClass = classLoader.loadClass(threadContextMap);
                    final Class<? extends ThreadContextMap> contextMapClass = mapClass.asSubclass(ThreadContextMap.class);
                    return () -> getInstance(contextMapClass);
                } catch (final ClassNotFoundException e) {
                    logger.error("Unable to create class {} specified in {}", threadContextMap, url, e);
                }
            }
        }
        return super.createContextMapFactory();
    }

    private static Properties loadFromUrl(final URL url) throws IOException {
        try (final InputStream in = url.openStream()) {
            final Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }
}

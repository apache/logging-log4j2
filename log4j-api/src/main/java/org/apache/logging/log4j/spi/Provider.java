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
package org.apache.logging.log4j.spi;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Model class for a Log4j 2 provider. The properties in this class correspond to the properties used in a
 * {@code META-INF/log4j-provider.properties} file. Note that this class is automatically created by Log4j and should
 * not be used by providers.
 */
public class Provider {
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

    private static final Integer DEFAULT_PRIORITY = Integer.valueOf(-1);
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Integer priority;
    private final String className;
    private final Class<? extends LoggerContextFactory> loggerContextFactoryClass;
    private final String threadContextMap;
    private final Class<? extends ThreadContextMap> threadContextMapClass;
    private final String versions;
    private final URL url;
    private final WeakReference<ClassLoader> classLoader;

    public Provider(final Properties props, final URL url, final ClassLoader classLoader) {
        this.url = url;
        this.classLoader = new WeakReference<>(classLoader);
        final String weight = props.getProperty(FACTORY_PRIORITY);
        priority = weight == null ? DEFAULT_PRIORITY : Integer.valueOf(weight);
        className = props.getProperty(LOGGER_CONTEXT_FACTORY);
        threadContextMap = props.getProperty(THREAD_CONTEXT_MAP);
        loggerContextFactoryClass = null;
        threadContextMapClass = null;
        versions = null;
    }

    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass) {
        this(priority, versions, loggerContextFactoryClass, null);
    }

    public Provider(
            final Integer priority,
            final String versions,
            final Class<? extends LoggerContextFactory> loggerContextFactoryClass,
            final Class<? extends ThreadContextMap> threadContextMapClass) {
        this.url = null;
        this.classLoader = null;
        this.priority = priority;
        this.loggerContextFactoryClass = loggerContextFactoryClass;
        this.threadContextMapClass = threadContextMapClass;
        this.className = null;
        this.threadContextMap = null;
        this.versions = versions;
    }

    /**
     * Returns the Log4j API versions supported by the implementation.
     * @return A String containing the Log4j versions supported.
     */
    public String getVersions() {
        return versions;
    }

    /**
     * Gets the priority (natural ordering) of this Provider.
     *
     * @return the priority of this Provider
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Gets the class name of the {@link org.apache.logging.log4j.spi.LoggerContextFactory} implementation of this
     * Provider.
     *
     * @return the class name of a LoggerContextFactory implementation
     */
    public String getClassName() {
        if (loggerContextFactoryClass != null) {
            return loggerContextFactoryClass.getName();
        }
        return className;
    }

    /**
     * Loads the {@link org.apache.logging.log4j.spi.LoggerContextFactory} class specified by this Provider.
     *
     * @return the LoggerContextFactory implementation class or {@code null} if there was an error loading it
     */
    public Class<? extends LoggerContextFactory> loadLoggerContextFactory() {
        if (loggerContextFactoryClass != null) {
            return loggerContextFactoryClass;
        }
        if (className == null) {
            return null;
        }
        final ClassLoader loader = classLoader.get();
        if (loader == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(className);
            if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(LoggerContextFactory.class);
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to create class {} specified in {}", className, url.toString(), e);
        }
        return null;
    }

    /**
     * Gets the class name of the {@link org.apache.logging.log4j.spi.ThreadContextMap} implementation of this Provider.
     *
     * @return the class name of a ThreadContextMap implementation
     */
    public String getThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass.getName();
        }
        return threadContextMap;
    }

    /**
     * Loads the {@link org.apache.logging.log4j.spi.ThreadContextMap} class specified by this Provider.
     *
     * @return the ThreadContextMap implementation class or {@code null} if there was an error loading it
     */
    public Class<? extends ThreadContextMap> loadThreadContextMap() {
        if (threadContextMapClass != null) {
            return threadContextMapClass;
        }
        if (threadContextMap == null) {
            return null;
        }
        final ClassLoader loader = classLoader.get();
        if (loader == null) {
            return null;
        }
        try {
            final Class<?> clazz = loader.loadClass(threadContextMap);
            if (ThreadContextMap.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(ThreadContextMap.class);
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to create class {} specified in {}", threadContextMap, url.toString(), e);
        }
        return null;
    }

    /**
     * Gets the URL containing this Provider's Log4j details.
     *
     * @return the URL corresponding to the Provider {@code META-INF/log4j-provider.properties} file
     */
    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder("Provider[");
        if (!DEFAULT_PRIORITY.equals(priority)) {
            result.append("priority=").append(priority).append(", ");
        }
        if (threadContextMap != null) {
            result.append("threadContextMap=").append(threadContextMap).append(", ");
        } else if (threadContextMapClass != null) {
            result.append("threadContextMapClass=").append(threadContextMapClass.getName());
        }
        if (className != null) {
            result.append("className=").append(className).append(", ");
        } else if (loggerContextFactoryClass != null) {
            result.append("class=").append(loggerContextFactoryClass.getName());
        }
        if (url != null) {
            result.append("url=").append(url);
        }
        final ClassLoader loader;
        if (classLoader == null || (loader = classLoader.get()) == null) {
            result.append(", classLoader=null(not reachable)");
        } else {
            result.append(", classLoader=").append(loader);
        }
        result.append("]");
        return result.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Provider)) {
            return false;
        }

        final Provider provider = (Provider) o;

        if (priority != null ? !priority.equals(provider.priority) : provider.priority != null) {
            return false;
        }
        if (className != null ? !className.equals(provider.className) : provider.className != null) {
            return false;
        }
        if (loggerContextFactoryClass != null
                ? !loggerContextFactoryClass.equals(provider.loggerContextFactoryClass)
                : provider.loggerContextFactoryClass != null) {
            return false;
        }
        return versions != null ? versions.equals(provider.versions) : provider.versions == null;
    }

    @Override
    public int hashCode() {
        int result = priority != null ? priority.hashCode() : 0;
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (loggerContextFactoryClass != null ? loggerContextFactoryClass.hashCode() : 0);
        result = 31 * result + (versions != null ? versions.hashCode() : 0);
        return result;
    }
}

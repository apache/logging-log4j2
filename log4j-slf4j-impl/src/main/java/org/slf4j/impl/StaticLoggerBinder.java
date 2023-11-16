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
package org.slf4j.impl;

import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.apache.logging.slf4j.Log4jMarkerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * SLF4J LoggerFactoryBinder implementation using Log4j. This class is part of the required classes used to specify an
 * SLF4J logger provider implementation.
 */
public final class StaticLoggerBinder implements LoggerFactoryBinder {

    /**
     * Declare the version of the SLF4J API this implementation is compiled
     * against. The value of this field is usually modified with each release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6"; // !final

    private static final String LOGGER_FACTORY_CLASS_STR = Log4jLoggerFactory.class.getName();

    /**
     * The unique instance of this class.
     */
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
     * method should always be the same object
     */
    private final ILoggerFactory loggerFactory;

    /**
     * Private constructor to prevent instantiation
     */
    private StaticLoggerBinder() {
        loggerFactory = new Log4jLoggerFactory(
                (Log4jMarkerFactory) StaticMarkerBinder.getSingleton().getMarkerFactory());
    }

    /**
     * Returns the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * Returns the factory.
     * @return the factor.
     */
    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    /**
     * Returns the class name.
     * @return the class name;
     */
    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_STR;
    }
}

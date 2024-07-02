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
package org.apache.logging.log4j.test.junit;

import aQute.bnd.annotation.baseline.BaselineIgnore;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Constants to use the {@link ResourceLock} annotation.
 */
@BaselineIgnore("2.24.0")
public final class Log4jStaticResources {

    /**
     * Marks tests that require access to {@link org.apache.logging.log4j.LogManager} methods or change its
     * underlying {@link org.apache.logging.log4j.spi.LoggerContextFactory} implementation.
     */
    public static final String LOG_MANAGER = "log4j.LogManager";

    /**
     * Marks tests that require access to {@link org.apache.logging.log4j.ThreadContext} methods or change its
     * underlying {@link org.apache.logging.log4j.spi.ThreadContextMap} implementation.
     */
    public static final String THREAD_CONTEXT = "log4j.ThreadContext";

    /**
     * Marks tests that require access to {@link org.apache.logging.log4j.MarkerManager} methods.
     */
    public static final String MARKER_MANAGER = "log4j.MarkerManager";

    /**
     * Marks tests that requires access to {@link org.apache.logging.log4j.Level} static methods to create new levels.
     */
    public static final String LEVEL = "log4j.Level";

    /**
     * Marks tests that require access to {@link org.apache.logging.log4j.status.StatusLogger} static methods or
     * change its underlying implementation.
     */
    public static final String STATUS_LOGGER = "log4j.StatusLogger";

    private Log4jStaticResources() {}
}

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

/**
 * Components that own properties.
 */
public enum PropertyComponent {
    ASYNC_LOGGER(Constant.ASYNC_LOGGER),
    ASYNC_LOGGER_CONFIG(Constant.ASYNC_LOGGER_CONFIG),
    CONFIGURATION(Constant.CONFIGURATION),
    CONSOLE(Constant.CONSOLE),
    GC(Constant.GC),
    JANSI(Constant.JANSI),
    JMX(Constant.JMX),
    JNDI(Constant.JNDI),
    JUL(Constant.JUL),
    LOADER(Constant.LOADER),
    LOG4J(Constant.LOG4J),
    LOG4J1(Constant.LOG4J1),
    LOGGER(Constant.LOGGER),
    LOGGER_CONTEXT(Constant.LOGGER_CONTEXT),
    MESSAGE(Constant.MESSAGE),
    RECYCLER(Constant.RECYCLER),
    SCRIPT(Constant.SCRIPT),
    SIMPLE_LOGGER(Constant.SIMPLE_LOGGER),
    STATUS_LOGGER(Constant.STATUS_LOGGER),
    SYSTEM(Constant.SYSTEM),
    THREAD_CONTEXT(Constant.THREAD_CONTEXT),
    TRANSPORT_SECURITY(Constant.TRANSPORT_SECURITY),
    UUID(Constant.UUID),
    WEB(Constant.WEB);

    private final String name;

    PropertyComponent(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * These constants are only for use in LoggingSystemProperty to create constants for @SetSystemProperty.
     */
    public static class Constant {
        public static final String ASYNC_LOGGER = "AsyncLogger";
        public static final String ASYNC_LOGGER_CONFIG = "AsyncLoggerConfig";
        public static final String CONFIGURATION = "Configuration";
        public static final String CONSOLE = "Console";
        public static final String GC = "GC";
        public static final String JANSI = "JANSI";
        public static final String JMX = "JMX";
        public static final String JNDI = "JNDI";
        public static final String JUL = "JUL";
        public static final String LOADER = "Loader";
        public static final String LOG4J = "log4j";
        public static final String LOG4J1 = "log4j1";
        public static final String LOGGER = "Logger";
        public static final String LOGGER_CONTEXT = "LoggerContext";
        public static final String MESSAGE = "Message";
        public static final String RECYCLER = "Recycler";
        public static final String SCRIPT = "Script";
        public static final String SIMPLE_LOGGER = "SimpleLogger";
        public static final String STATUS_LOGGER = "StatusLogger";
        public static final String SYSTEM = "System";
        public static final String THREAD_CONTEXT = "ThreadContext";
        public static final String TRANSPORT_SECURITY = "TransportSecurity";
        public static final String UUID = "UUID";
        public static final String WEB = "Web";
    }
}

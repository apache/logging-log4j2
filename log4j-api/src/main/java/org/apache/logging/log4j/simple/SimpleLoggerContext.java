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
package org.apache.logging.log4j.simple;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * A simple {@link LoggerContext} implementation.
 */
public class SimpleLoggerContext implements LoggerContext {

    /** Singleton instance. */
    static final SimpleLoggerContext INSTANCE = new SimpleLoggerContext();

    private static final String SYSTEM_OUT = "system.out";

    private static final String SYSTEM_ERR = "system.err";

    /** The default format to use when formatting dates */
    protected static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";

    /** All system properties used by <code>SimpleLog</code> start with this */
    protected static final String SYSTEM_PREFIX = "org.apache.logging.log4j.simplelog.";

    private final SimpleLoggerConfiguration configuration;

    private final PrintStream stream;

    private final LoggerRegistry<ExtendedLogger> loggerRegistry = new LoggerRegistry<>();

    /**
     * Constructs a new initialized instance.
     */
    public SimpleLoggerContext() {
        this(new SimpleLoggerConfiguration(PropertiesUtil.getProperties("simplelog")));
    }

    public SimpleLoggerContext(final SimpleLoggerConfiguration configuration) {
        this.configuration = configuration;
        final String fileName = configuration.getLogFileName();
        PrintStream ps;
        if (SYSTEM_ERR.equalsIgnoreCase(fileName)) {
            ps = System.err;
        } else if (SYSTEM_OUT.equalsIgnoreCase(fileName)) {
            ps = System.out;
        } else {
            try {
                ps = new PrintStream(new FileOutputStream(fileName));
            } catch (final FileNotFoundException fnfe) {
                ps = System.err;
            }
        }
        this.stream = ps;
    }

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        return getLogger(name, null);
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        // Note: This is the only method where we add entries to the 'loggerRegistry' ivar.
        final ExtendedLogger extendedLogger = loggerRegistry.getLogger(name, messageFactory);
        if (extendedLogger != null) {
            AbstractLogger.checkMessageFactory(extendedLogger, messageFactory);
            return extendedLogger;
        }
        final SimpleLogger simpleLogger = new SimpleLogger(name, messageFactory, stream, configuration);
        loggerRegistry.putIfAbsent(name, messageFactory, simpleLogger);
        return loggerRegistry.getLogger(name, messageFactory);
    }

    /**
     * Gets the LoggerRegistry.
     *
     * @return the LoggerRegistry.
     * @since 2.17.2
     */
    @Override
    public LoggerRegistry<ExtendedLogger> getLoggerRegistry() {
        return loggerRegistry;
    }

    @Override
    public boolean hasLogger(final String name) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return false;
    }

}

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
package org.apache.logging.log4j.test.appender;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Throwables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * An {@link Appender} that fails on the first use and works for the rest.
 */
@Plugin(name="FailOnce", category ="Core", elementType=Appender.ELEMENT_TYPE, printObject=true)
public class FailOnceAppender extends AbstractAppender {

    private final Supplier<Throwable> throwableSupplier;

    private boolean failed = false;

    private List<LogEvent> events = new ArrayList<>();

    private FailOnceAppender(final String name, final Supplier<Throwable> throwableSupplier) {
        super(name, null, null, false, Property.EMPTY_ARRAY);
        this.throwableSupplier = throwableSupplier;
    }

    @Override
    public synchronized void append(final LogEvent event) {
        if (!failed) {
            failed = true;
            Throwable throwable = throwableSupplier.get();
            Throwables.rethrow(throwable);
        }
        events.add(event);
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    /**
     * Returns the list of accumulated events and resets the internal buffer.
     */
    public synchronized List<LogEvent> drainEvents() {
        final List<LogEvent> oldEvents = events;
        this.events = new ArrayList<>();
        return oldEvents;
    }

    @PluginFactory
    public static FailOnceAppender createAppender(
        @PluginAttribute("name") @Required(message = "A name for the Appender must be specified") final String name,
        @PluginAttribute("throwableClassName") final String throwableClassName) {
        final Supplier<Throwable> throwableSupplier = createThrowableSupplier(name, throwableClassName);
        return new FailOnceAppender(name, throwableSupplier);
    }

    private static Supplier<Throwable> createThrowableSupplier(
            final String name,
            final String throwableClassName) {

        // Fallback to LoggingException if none is given.
        final String message = String.format("failing on purpose for appender '%s'", name);
        if (throwableClassName == null || ThrowableClassName.LOGGING_EXCEPTION.equals(throwableClassName)) {
            return () -> new LoggingException(message);
        }

        // Check against the expected exception classes.
        switch (throwableClassName) {
            case ThrowableClassName.RUNTIME_EXCEPTION: return () -> new RuntimeException(message);
            case ThrowableClassName.EXCEPTION: return () -> new Exception(message);
            case ThrowableClassName.ERROR: return () -> new Error(message);
            case ThrowableClassName.THROWABLE: return () -> new Throwable(message);
            case ThrowableClassName.THREAD_DEATH: return () -> {
                stopCurrentThread();
                throw new IllegalStateException("should not have reached here");
            };
            default: throw new IllegalArgumentException("unknown throwable class name: " + throwableClassName);
        }

    }

    @SuppressWarnings("deprecation")
    private static void stopCurrentThread() {
        Thread.currentThread().stop();
    }

    public enum ThrowableClassName {;

        public static final String RUNTIME_EXCEPTION = "RuntimeException";

        public static final String LOGGING_EXCEPTION = "LoggingException";

        public static final String EXCEPTION = "Exception";

        public static final String ERROR = "Error";

        public static final String THROWABLE = "Throwable";

        public static final String THREAD_DEATH = "ThreadDeath";

    }

}

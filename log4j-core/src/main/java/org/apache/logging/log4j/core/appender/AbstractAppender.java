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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Objects;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Integers;

/**
 * Abstract base class for Appenders. Although Appenders do not have to extend this class, doing so will simplify their
 * implementation.
 */
public abstract class AbstractAppender extends AbstractFilterable implements Appender, LocationAware {

    /**
     * Subclasses can extend this abstract Builder.
     *
     * @param <B> The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B> {

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout;

        @PluginBuilderAttribute
        @Required(message = "No appender name provided")
        private String name;

        @PluginConfiguration
        private Configuration configuration;

        public Configuration getConfiguration() {
            return configuration;
        }

        public Layout<? extends Serializable> getLayout() {
            return layout;
        }

        public String getName() {
            return name;
        }

        public Layout<? extends Serializable> getOrCreateLayout() {
            if (layout == null) {
                return PatternLayout.createDefaultLayout(configuration);
            }
            return layout;
        }

        public Layout<? extends Serializable> getOrCreateLayout(final Charset charset) {
            if (layout == null) {
                return PatternLayout.newBuilder()
                        .withCharset(charset)
                        .withConfiguration(configuration)
                        .build();
            }
            return layout;
        }

        public boolean isIgnoreExceptions() {
            return ignoreExceptions;
        }

        public B setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return asBuilder();
        }

        public B setIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return asBuilder();
        }

        public B setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return asBuilder();
        }

        public B setName(final String name) {
            this.name = name;
            return asBuilder();
        }

        /**
         * @deprecated Use {@link #setConfiguration(Configuration)}
         */
        @Deprecated
        public B withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return asBuilder();
        }

        /**
         * @deprecated use {@link #setIgnoreExceptions(boolean)}.
         */
        @Deprecated
        public B withIgnoreExceptions(final boolean ignoreExceptions) {
            return setIgnoreExceptions(ignoreExceptions);
        }

        /**
         * @deprecated use {@link #setLayout(Layout)}.
         */
        @Deprecated
        public B withLayout(final Layout<? extends Serializable> layout) {
            return setLayout(layout);
        }

        /**
         * @deprecated use {@link #setName(String)}.
         */
        @Deprecated
        public B withName(final String name) {
            return setName(name);
        }

        public String getErrorPrefix() {
            final Class<?> appenderClass = getClass().getEnclosingClass();
            final String name = getName();
            final StringBuilder sb =
                    new StringBuilder(appenderClass != null ? appenderClass.getSimpleName() : "Appender");
            if (name != null) {
                sb.append(" '").append(name).append("'");
            }
            return sb.toString();
        }
    }

    public static int parseInt(final String s, final int defaultValue) {
        try {
            return Integers.parseInt(s, defaultValue);
        } catch (final NumberFormatException e) {
            LOGGER.error("Could not parse \"{}\" as an integer,  using default value {}: {}", s, defaultValue, e);
            return defaultValue;
        }
    }

    private final String name;
    private final boolean ignoreExceptions;
    private final Layout<? extends Serializable> layout;

    private ErrorHandler handler = new DefaultErrorHandler(this);

    @Override
    public boolean requiresLocation() {
        return layout instanceof LocationAware && ((LocationAware) layout).requiresLocation();
    }

    /**
     * Constructor that defaults to suppressing exceptions.
     *
     * @param name The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     * @deprecated Use {@link #AbstractAppender(String, Filter, Layout, boolean, Property[])}.
     */
    @Deprecated
    protected AbstractAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout) {
        this(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    /**
     * Constructor.
     *
     * @param name The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     * @param ignoreExceptions If true, exceptions will be logged and suppressed. If false errors will be logged and
     *            then passed to the application.
     * @deprecated Use {@link #AbstractAppender(String, Filter, Layout, boolean, Property[])}
     */
    @Deprecated
    protected AbstractAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions) {
        this(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    /**
     * Constructor.
     *
     * @param name The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     * @param ignoreExceptions If true, exceptions will be logged and suppressed. If false errors will be logged and
     *            then passed to the application.
     * @since 2.11.2
     */
    protected AbstractAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(filter, properties);
        this.name = Objects.requireNonNull(name, "name");
        this.layout = layout;
        this.ignoreExceptions = ignoreExceptions;
    }

    /**
     * Handle an error with a message using the {@link ErrorHandler} configured for this Appender.
     *
     * @param msg The message.
     */
    public void error(final String msg) {
        handler.error(msg);
    }

    /**
     * Handle an error with a message, exception, and a logging event, using the {@link ErrorHandler} configured for
     * this Appender.
     *
     * @param msg The message.
     * @param event The LogEvent.
     * @param t The Throwable.
     */
    public void error(final String msg, final LogEvent event, final Throwable t) {
        handler.error(msg, event, t);
    }

    /**
     * Handle an error with a message and an exception using the {@link ErrorHandler} configured for this Appender.
     *
     * @param msg The message.
     * @param t The Throwable.
     */
    public void error(final String msg, final Throwable t) {
        handler.error(msg, t);
    }

    /**
     * Returns the ErrorHandler, if any.
     *
     * @return The ErrorHandler.
     */
    @Override
    public ErrorHandler getHandler() {
        return handler;
    }

    /**
     * Returns the Layout for the appender.
     *
     * @return The Layout used to format the event.
     */
    @Override
    public Layout<? extends Serializable> getLayout() {
        return layout;
    }

    /**
     * Returns the name of the Appender.
     *
     * @return The name of the Appender.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Some appenders need to propagate exceptions back to the application. When {@code ignoreExceptions} is
     * {@code false} the AppenderControl will allow the exception to percolate.
     *
     * @return {@code true} if exceptions will be logged but now thrown, {@code false} otherwise.
     */
    @Override
    public boolean ignoreExceptions() {
        return ignoreExceptions;
    }

    /**
     * The handler must be set before the appender is started.
     *
     * @param handler The ErrorHandler to use.
     */
    @Override
    public void setHandler(final ErrorHandler handler) {
        if (handler == null) {
            LOGGER.error("The handler cannot be set to null");
            return;
        }
        if (isStarted()) {
            LOGGER.error("The handler cannot be changed once the appender is started");
            return;
        }
        this.handler = handler;
    }

    /**
     * Serializes the given event using the appender's layout if present.
     *
     * @param event
     *            the event to serialize.
     * @return the serialized event or null if no layout is present.
     */
    protected Serializable toSerializable(final LogEvent event) {
        return layout != null ? layout.toSerializable(event) : null;
    }

    @Override
    public String toString() {
        return name;
    }
}

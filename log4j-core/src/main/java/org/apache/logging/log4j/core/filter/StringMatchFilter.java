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
package org.apache.logging.log4j.core.filter;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.util.Assert;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This filter returns the {@code onMatch} result if the formatted message contains the
 * configured "{@code text}" value; otherwise, it returns the {@code onMismatch} result.
 * <p>
 *   The text comparison is case-sensitive.
 * </p>
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@NullMarked
@Plugin
@PerformanceSensitive("allocation")
public final class StringMatchFilter extends AbstractFilter {

    /** The string match text. */
    private final String text;

    /**
     * Constructs a new string-match filter instance.
     *
     * @param builder the builder implementation
     * @throws IllegalArgumentException if the {@code text} argument is {@code null} or blank
     */
    private StringMatchFilter(final Builder builder) {

        super(builder);

        if (Strings.isNotEmpty(builder.text)) {
            this.text = builder.text;
        } else {
            throw new IllegalArgumentException("The 'text' argument must not be null or empty.");
        }

    }

    /**
     * Returns the string-filter match text
     * @return the match text
     */
    public String getText() {
        return this.text;
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation on the given event's formatted message.
     * </p>
     *
     * @throws NullPointerException if the given {@code event} is {@code null}
     */
    @Override
    public Result filter(final LogEvent event) {
        Objects.requireNonNull(event, "The 'event' argument must not be null.");
        return filter(event.getMessage().getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable String msg,
            final @Nullable Object @Nullable ... params) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(msg, params).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against a new message  the logger's message-factory to create a new {@link Message} and perform
     *   the filter action against this filter's match text.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *     <li>{@code throwable}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable Object message,
            final @Nullable Throwable throwable) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(message).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the provided {@link Message}'s
     *   formatted message.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code logger}</li>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *     <li>{@code throwable}</li>
     *   </ul>
     * </p>
     *
     * @param logger the logger or {@code null} (<i>unused</i>)
     * @param level the logging level or {@code null} (<i>unused</i>)
     * @param marker the marker or {@code null} (<i>unused</i>)
     * @param message the message
     * @param throwable a throwable or {@code null} (<i>unused</i>)
     * @return the filter result
     * @throws NullPointerException if the {@code message} argument is {@code null}
     */
    @Override
    public Result filter(
            final @Nullable Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final Message message,
            final @Nullable Throwable throwable /* unused */) {
        Objects.requireNonNull(message, "The 'msg' argument must not be null.");
        return filter(message.getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String message,
            final @Nullable Object p0) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(message, p0).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1, p2).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1, p2, p3).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(
                logger.getMessageFactory().newMessage(msg, p0, p1, p2, p3, p4).getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4,
            final @Nullable Object p5) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5)
                .getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4,
            final @Nullable Object p5,
            final @Nullable Object p6) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6)
                .getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4,
            final @Nullable Object p5,
            final @Nullable Object p6,
            final @Nullable Object p7) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7)
                .getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4,
            final @Nullable Object p5,
            final @Nullable Object p6,
            final @Nullable Object p7,
            final @Nullable Object p8) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7, p8)
                .getFormattedMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with the
     *   given parameters.
     * </p>
     * <p>
     *   The following parameters are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     *
     * @throws NullPointerException if the {@code logger} argument is {@code null}
     */
    @Override
    public Result filter(
            final Logger logger,
            final @Nullable Level level, // unused
            final @Nullable Marker marker, // unused
            final @Nullable String msg,
            final @Nullable Object p0,
            final @Nullable Object p1,
            final @Nullable Object p2,
            final @Nullable Object p3,
            final @Nullable Object p4,
            final @Nullable Object p5,
            final @Nullable Object p6,
            final @Nullable Object p7,
            final @Nullable Object p8,
            final @Nullable Object p9) {
        Objects.requireNonNull(logger, "The 'logger' argument must not be null.");
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)
                .getFormattedMessage());
    }

    /**
     * Evaluates the filter result for the given message.
     * <p>
     *   If the given {@code message} is {@code null}, this method will always return the mismatch result.
     * </p>
     * @param message the message to evaluate
     * @return the configured match result if the message contains the string-match filter text;
     *         otherwise, the configured mismatch result
     */
    private Result filter(final @Nullable String message) {
        return (message != null && message.contains(this.text)) ? onMatch : onMismatch;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Creates a new builder instance.
     * @return the new builder instance
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /** A {@link StringMatchFilter} builder implementation. */
    public static class Builder extends AbstractFilterBuilder<StringMatchFilter.Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<StringMatchFilter> {

        @PluginBuilderAttribute
        @Required(message = "No text provided for StringMatchFilter")
        private @Nullable String text;

        /** Private constructor. */
        private Builder() {
            super();
        }

        /**
         * Sets the text to search in event messages.
         * @param text the text to search in event messages.
         * @return this instance.
         */
        public Builder setText(final String text) {
            this.text = Assert.requireNonEmpty(text, "The 'text' argument must not be null or empty.");
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public @Nullable StringMatchFilter build() {

            // validate the 'text' attribute
            if (this.text == null) {
                LOGGER.error("Unable to create StringMatchFilter: The 'text' attribute must be configured.");
                return null;
            }

            // build with *safety* to not throw unexpected exceptions
            try {
                return new StringMatchFilter(this);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create StringMatchFilter: {}", ex.getMessage(), ex);
                return null;
            }

        }
    }
}

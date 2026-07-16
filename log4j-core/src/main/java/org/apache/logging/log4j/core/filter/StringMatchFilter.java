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
import org.jspecify.annotations.NonNull;

/**
 * This filter returns the onMatch result if the message in the event matches the specified text
 * exactly.
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class StringMatchFilter extends AbstractFilter {

    private final String text;

    private StringMatchFilter(final Builder builder) {
        super(builder.getOnMatch(), builder.getOnMismatch());
        this.text = Assert.requireNonEmpty(builder.text, "The 'text' argument must not be null or empty.");
    }

    public String getText() {
        return text;
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter(logger.getMessageFactory().newMessage(msg, params).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter(logger.getMessageFactory().newMessage(msg).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter(msg.getFormattedMessage());
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getMessage().getFormattedMessage());
    }

    private Result filter(final String msg) {
        return msg.contains(this.text) ? onMatch : onMismatch;
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter(logger.getMessageFactory().newMessage(msg, p0).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1, p2).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter(logger.getMessageFactory().newMessage(msg, p0, p1, p2, p3).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter(
                logger.getMessageFactory().newMessage(msg, p0, p1, p2, p3, p4).getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5)
                .getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6)
                .getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7)
                .getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7, p8)
                .getFormattedMessage());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter(logger.getMessageFactory()
                .newMessage(msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)
                .getFormattedMessage());
    }

    @Override
    public String toString() {
        return text;
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterBuilder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<StringMatchFilter> {

        @PluginBuilderAttribute
        @Required(message = "No text provided for StringMatchFilter")
        private String text;

        /**
         * Sets the text to search in event messages.
         * @param text the text to search in event messages.
         * @return this instance.
         */
        public Builder setText(@NonNull final String text) {
            Objects.requireNonNull(text, "The 'text' argument must not be null.");
            this.text = Assert.requireNonEmpty(text, "The 'text' argument must not be empty.");
            return this;
        }

        @Override
        public StringMatchFilter build() {
            if (this.text == null) {
                LOGGER.error("Unable to create StringMatchFilter: The 'text' attribute must be configured.");
                return null;
            }
            return new StringMatchFilter(this);
        }
    }
}

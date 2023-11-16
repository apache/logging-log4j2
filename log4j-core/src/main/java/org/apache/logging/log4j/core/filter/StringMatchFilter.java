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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * This filter returns the onMatch result if the logging level in the event matches the specified logging level
 * exactly.
 */
@Plugin(name = "StringMatchFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@PerformanceSensitive("allocation")
public final class StringMatchFilter extends AbstractFilter {

    public static final String ATTR_MATCH = "match";
    private final String text;

    private StringMatchFilter(final String text, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.text = text;
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

    @PluginBuilderFactory
    public static StringMatchFilter.Builder newBuilder() {
        return new StringMatchFilter.Builder();
    }

    public static class Builder extends AbstractFilterBuilder<StringMatchFilter.Builder>
            implements org.apache.logging.log4j.core.util.Builder<StringMatchFilter> {
        @PluginBuilderAttribute
        private String text = "";

        /**
         * Sets the logging level to use.
         * @param text the logging level to use
         * @return this
         */
        public StringMatchFilter.Builder setMatchString(final String text) {
            this.text = text;
            return this;
        }

        @Override
        public StringMatchFilter build() {
            return new StringMatchFilter(this.text, this.getOnMatch(), this.getOnMismatch());
        }
    }
}

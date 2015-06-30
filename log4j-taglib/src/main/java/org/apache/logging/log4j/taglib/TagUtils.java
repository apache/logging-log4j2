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
package org.apache.logging.log4j.taglib;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Provides support for logging tags.
 *
 * @since 2.0
 */
final class TagUtils {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final Set<Object> WARNED_FOR = new HashSet<>();

    private static final String LOGGER_SCOPE_ATTRIBUTE = "org.apache.logging.log4j.taglib.LOGGER_SCOPE_ATTRIBUTE";

    private TagUtils() {
        throw new RuntimeException("TagUtils cannot be instantiated.");
    }

    static int getScope(final String scope) {
        if ("request".equalsIgnoreCase(scope)) {
            return PageContext.REQUEST_SCOPE;
        }
        if ("session".equalsIgnoreCase(scope)) {
            return PageContext.SESSION_SCOPE;
        }
        if ("application".equalsIgnoreCase(scope)) {
            return PageContext.APPLICATION_SCOPE;
        }
        return PageContext.PAGE_SCOPE;
    }

    static Level resolveLevel(final Object level) {
        if (level instanceof Level) {
            return (Level) level;
        }
        if (level instanceof String) {
            return Level.toLevel((String) level);
        }
        return null;
    }

    static Log4jTaglibLogger resolveLogger(final Log4jTaglibLoggerContext context, final Object logger,
                                           final MessageFactory factory) throws JspException {
        if (logger instanceof Logger) {
            if (logger instanceof Log4jTaglibLogger) {
                return (Log4jTaglibLogger) logger;
            }
            if (logger instanceof AbstractLogger) {
                if (LOGGER.isInfoEnabled() && !WARNED_FOR.contains(logger)) {
                    LOGGER.info("Constructing new Log4jTaglibLogger from AbstractLogger {} name and message factory.",
                            logger.getClass().getName());
                    WARNED_FOR.add(logger);
                }
                final AbstractLogger original = (AbstractLogger) logger;
                return getLogger(context, original.getName(), original.getMessageFactory());
            }
            throw new JspException(
                    "Log4j Tag Library requires base logging system to extend Log4j AbstractLogger.");
        }
        if (logger instanceof String) {
            return getLogger(context, (String) logger, factory);
        }
        throw new JspException("Logger must be of type String or org.apache.logging.log4j.Logger.");
    }

    private static Log4jTaglibLogger getLogger(final Log4jTaglibLoggerContext context, final String name,
                                               final MessageFactory factory)
            throws JspException {
        try {
            return context.getLogger(name, factory);
        } catch (final LoggingException e) {
            throw new JspException(e.getMessage(), e);
        }
    }

    static void setDefaultLogger(final PageContext pageContext, final Log4jTaglibLogger logger) {
        pageContext.setAttribute(TagUtils.LOGGER_SCOPE_ATTRIBUTE, logger, PageContext.PAGE_SCOPE);
    }

    static Log4jTaglibLogger getDefaultLogger(final PageContext pageContext) {
        return (Log4jTaglibLogger) pageContext.getAttribute(TagUtils.LOGGER_SCOPE_ATTRIBUTE, PageContext.PAGE_SCOPE);
    }

    static boolean isEnabled(final Log4jTaglibLogger logger, final Level level, final Marker marker) {
        if (marker == null) {
            return logger.isEnabled(level);
        }
        return logger.isEnabled(level, marker, (Object) null, null);
    }
}

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
package org.apache.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Placeholder for Log4j 1.2 Console Appender.
 */
public class ConsoleAppender extends WriterAppender {

    public static final String SYSTEM_OUT = "System.out";
    public static final String SYSTEM_ERR = "System.err";

    protected String target = SYSTEM_OUT;

    /**
     * Determines if the appender honors reassignments of System.out or System.err made after configuration.
     */
    private boolean follow;

    /**
     * Constructs a non-configured appender.
     */
    public ConsoleAppender() {}

    /**
     * Constructs a configured appender.
     *
     * @param layout layout, may not be null.
     */
    public ConsoleAppender(final Layout layout) {
        this(layout, SYSTEM_OUT);
    }

    /**
     * Constructs a configured appender.
     *
     * @param layout layout, may not be null.
     * @param target target, either "System.err" or "System.out".
     */
    public ConsoleAppender(final Layout layout, final String target) {
        setLayout(layout);
        setTarget(target);
        activateOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void append(final LoggingEvent theEvent) {
        // NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * Gets whether the appender honors reassignments of System.out or System.err made after configuration.
     *
     * @return true if appender will use value of System.out or System.err in force at the time when logging events are
     *         appended.
     * @since 1.2.13
     */
    public boolean getFollow() {
        return follow;
    }

    /**
     * Gets the current value of the <b>Target</b> property. The default value of the option is "System.out".
     *
     * See also {@link #setTarget}.
     */
    public String getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Sets whether the appender honors reassignments of System.out or System.err made after configuration.
     *
     * @param follow if true, appender will use value of System.out or System.err in force at the time when logging events
     *        are appended.
     * @since 1.2.13
     */
    public void setFollow(final boolean follow) {
        this.follow = follow;
    }

    /**
     * Sets the value of the <b>Target</b> option. Recognized values are "System.out" and "System.err". Any other value will
     * be ignored.
     */
    public void setTarget(final String value) {
        final String v = value.trim();

        if (SYSTEM_OUT.equalsIgnoreCase(v)) {
            target = SYSTEM_OUT;
        } else if (SYSTEM_ERR.equalsIgnoreCase(v)) {
            target = SYSTEM_ERR;
        } else {
            targetWarn(value);
        }
    }

    void targetWarn(final String val) {
        StatusLogger.getLogger().warn("[" + val + "] should be System.out or System.err.");
        StatusLogger.getLogger().warn("Using previously set target, System.out by default.");
    }
}

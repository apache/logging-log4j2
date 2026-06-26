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
package org.apache.logging.log4j.core.appender.rolling;

import java.util.Objects;
import org.apache.logging.log4j.core.appender.rolling.action.Action;

/**
 * Description of actions needed to complete rollover.
 */
public final class RolloverDescriptionImpl implements RolloverDescription {
    /**
     * Active log file name after rollover.
     */
    private final String activeFileName;

    /**
     * Should active file be opened for appending.
     */
    private final boolean append;

    /**
     * Action to be completed after close of current active log file
     * before returning control to caller.
     */
    private final Action synchronous;

    /**
     * Action to be completed after close of current active log file
     * and before next rollover attempt, may be executed asynchronously.
     */
    private final Action asynchronous;

    /**
     * Minimum delay in seconds before the asynchronous action is scheduled.
     */
    private final int minAsyncDelay;

    /**
     * Maximum delay in seconds before the asynchronous action is scheduled.
     * The actual delay is a random value in [minAsyncDelay, maxAsyncDelay].
     */
    private final int maxAsyncDelay;

    /**
     * Create new instance with no async delay (immediate execution).
     *
     * @param activeFileName active log file name after rollover, may not be null.
     * @param append         true if active log file after rollover should be opened for appending.
     * @param synchronous    action to be completed after close of current active log file, may be null.
     * @param asynchronous   action to be completed after close of current active log file and
     *                       before next rollover attempt.
     */
    public RolloverDescriptionImpl(
            final String activeFileName, final boolean append, final Action synchronous, final Action asynchronous) {
        this(activeFileName, append, synchronous, asynchronous, 0, 0);
    }

    /**
     * Create new instance with a random async delay in the range {@code [minAsyncDelay, maxAsyncDelay]}.
     *
     * @param activeFileName active log file name after rollover, may not be null.
     * @param append         true if active log file after rollover should be opened for appending.
     * @param synchronous    action to be completed after close of current active log file, may be null.
     * @param asynchronous   action to be completed after close of current active log file and
     *                       before next rollover attempt.
     * @param minAsyncDelay  minimum delay in seconds before the asynchronous action is scheduled (0 = immediate).
     * @param maxAsyncDelay  maximum delay in seconds before the asynchronous action is scheduled (0 = immediate).
     * @since 2.26.0
     */
    public RolloverDescriptionImpl(
            final String activeFileName,
            final boolean append,
            final Action synchronous,
            final Action asynchronous,
            final int minAsyncDelay,
            final int maxAsyncDelay) {
        Objects.requireNonNull(activeFileName, "activeFileName");
        if (minAsyncDelay < 0) {
            throw new IllegalArgumentException("minAsyncDelay must be >= 0, got: " + minAsyncDelay);
        }
        if (maxAsyncDelay < 0) {
            throw new IllegalArgumentException("maxAsyncDelay must be >= 0, got: " + maxAsyncDelay);
        }
        if (maxAsyncDelay < minAsyncDelay) {
            throw new IllegalArgumentException(
                    "maxAsyncDelay (" + maxAsyncDelay + ") must be >= minAsyncDelay (" + minAsyncDelay + ")");
        }

        this.append = append;
        this.activeFileName = activeFileName;
        this.synchronous = synchronous;
        this.asynchronous = asynchronous;
        this.minAsyncDelay = minAsyncDelay;
        this.maxAsyncDelay = maxAsyncDelay;
    }

    /**
     * Active log file name after rollover.
     *
     * @return active log file name after rollover.
     */
    @Override
    public String getActiveFileName() {
        return activeFileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAppend() {
        return append;
    }

    /**
     * Action to be completed after close of current active log file
     * before returning control to caller.
     *
     * @return action, may be null.
     */
    @Override
    public Action getSynchronous() {
        return synchronous;
    }

    /**
     * Action to be completed after close of current active log file
     * and before next rollover attempt, may be executed asynchronously.
     *
     * @return action, may be null.
     */
    @Override
    public Action getAsynchronous() {
        return asynchronous;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinAsyncDelay() {
        return minAsyncDelay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxAsyncDelay() {
        return maxAsyncDelay;
    }
}

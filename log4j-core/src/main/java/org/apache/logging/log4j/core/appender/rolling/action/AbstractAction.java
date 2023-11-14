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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;


/**
 * Abstract base class for implementations of Action.
 */
public abstract class AbstractAction implements Action {

    /**
     * Allows subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();
    /**
     * Is action complete.
     */
    private boolean complete = false;

    /**
     * Is action interrupted.
     */
    private boolean interrupted = false;

    private final Lock lock = new ReentrantLock();

    /**
     * Constructor.
     */
    protected AbstractAction() {
    }

    /**
     * Performs action.
     *
     * @return true if successful.
     * @throws IOException if IO error.
     */
    @Override
    public abstract boolean execute() throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        lock.lock();
        try {
            if (!interrupted) {
                try {
                    execute();
                } catch (final RuntimeException | IOException ex) {
                    reportException(ex);
                } catch (final Error e) {
                    // reportException takes Exception, widening to Throwable would break custom implementations
                    // so we wrap Errors in RuntimeException for handling.
                    reportException(new RuntimeException(e));
                }

                complete = true;
                interrupted = true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        lock.lock();
        try {
            interrupted = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Tests if the action is complete.
     *
     * @return true if action is complete.
     */
    @Override
    public boolean isComplete() {
        return complete;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Captures exception.
     *
     * @param ex exception.
     */
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception reported by action '{}'", getClass(), ex);
    }

}

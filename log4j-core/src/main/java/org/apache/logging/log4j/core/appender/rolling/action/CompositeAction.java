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
import java.util.Arrays;
import java.util.List;

/**
 * A group of Actions to be executed in sequence.
 */
public class CompositeAction extends AbstractAction {

    /**
     * Actions to perform.
     */
    private final Action[] actions;

    /**
     * Stop on error.
     */
    private final boolean stopOnError;

    /**
     * Construct a new composite action.
     *
     * @param actions     list of actions, may not be null.
     * @param stopOnError if true, stop on the first false return value or exception.
     */
    public CompositeAction(final List<Action> actions, final boolean stopOnError) {
        this.actions = new Action[actions.size()];
        actions.toArray(this.actions);
        this.stopOnError = stopOnError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            execute();
        } catch (final IOException ex) {
            LOGGER.warn("Exception during file rollover.", ex);
        }
    }

    /**
     * Execute sequence of actions.
     *
     * @return true if all actions were successful.
     * @throws IOException on IO error.
     */
    @Override
    public boolean execute() throws IOException {
        if (stopOnError) {
            for (final Action action : actions) {
                if (!action.execute()) {
                    return false;
                }
            }

            return true;
        }
        boolean status = true;
        IOException exception = null;

        for (final Action action : actions) {
            try {
                status &= action.execute();
            } catch (final IOException ex) {
                status = false;

                if (exception == null) {
                    exception = ex;
                }
            }
        }

        if (exception != null) {
            throw exception;
        }

        return status;
    }

    @Override
    public String toString() {
        return CompositeAction.class.getSimpleName() + Arrays.toString(actions);
    }

    public Action[] getActions() {
        return actions;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }
}

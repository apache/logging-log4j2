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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper action that provides delay configuration for compression actions.
 * This action wraps another action and specifies a delay time, allowing
 * the scheduling to be handled externally by RollingFileManager.
 */
public class DelayedCompressionAction extends AbstractAction implements Schedulable {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Action originalAction;
    private final int delaySeconds;

    /**
     * Creates a new DelayedCompressionAction.
     *
     * @param originalAction the action to be executed with delay
     * @param delaySeconds the delay in seconds (0 means immediate execution)
     */
    public DelayedCompressionAction(Action originalAction, int delaySeconds) {
        this.originalAction = requireNonNull(originalAction, "originalAction");
        this.delaySeconds = delaySeconds;
    }

    /**
     * Executes the wrapped action immediately.
     * The delay is handled externally by RollingFileManager based on the
     * getDelaySeconds() method.
     *
     * @return true if the action was successfully executed
     * @throws IOException if an error occurs during execution
     */
    @Override
    public boolean execute() throws IOException {
        String sourceFile = extractSourceFileName(originalAction);

        try {
            LOGGER.debug("Starting delayed compression of {}", sourceFile);
            boolean success = originalAction.execute();
            if (success) {
                LOGGER.debug("Successfully completed delayed compression of {}", sourceFile);
            } else {
                LOGGER.warn("Failed to execute delayed compression of {}", sourceFile);
            }
            return success;
        } catch (Exception e) {
            LOGGER.warn("Exception during delayed compression of {}", sourceFile, e);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }
    }

    /**
     * Attempts to extract the source file name from the action for logging purposes.
     *
     * @param action the action to extract file name from
     * @return the source file name or action toString if cannot be determined
     */
    private String extractSourceFileName(Action action) {
        // This is a best-effort attempt to get the source file name
        // The actual implementation may need to be enhanced based on the action type
        return action.toString();
    }

    /**
     * Gets the original action being wrapped.
     *
     * @return the original action
     */
    public Action getOriginalAction() {
        return originalAction;
    }

    /**
     * Gets the delay in seconds for compression.
     *
     * @return the delay in seconds
     */
    public int getDelaySeconds() {
        return delaySeconds;
    }

    @Override
    public boolean isComplete() {
        // For simplicity, we consider this action complete after execution
        // In a more sophisticated implementation, we might track the wrapped action's completion
        return true;
    }

    @Override
    public String toString() {
        return "DelayedCompressionAction[originalAction=" + originalAction + ", delaySeconds=" + delaySeconds + "]";
    }
}


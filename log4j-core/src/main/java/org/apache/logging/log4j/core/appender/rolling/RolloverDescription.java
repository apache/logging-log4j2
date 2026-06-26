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

import org.apache.logging.log4j.core.appender.rolling.action.Action;

/**
 * Description of actions needed to complete rollover.
 */
public interface RolloverDescription {
    /**
     * Active log file name after rollover.
     *
     * @return active log file name after rollover.
     */
    String getActiveFileName();

    /**
     * Specifies if active file should be opened for appending.
     *
     * @return if true, active file should be opened for appending.
     */
    boolean getAppend();

    /**
     * Action to be completed after close of current active log file
     * before returning control to caller.
     *
     * @return action, may be null.
     */
    Action getSynchronous();

    /**
     * Action to be completed after close of current active log file
     * and before next rollover attempt, may be executed asynchronously.
     *
     * @return action, may be null.
     */
    Action getAsynchronous();

    /**
     * The minimum delay in seconds before the asynchronous action should be executed.
     * A value of 0 means the action should be executed immediately.
     * The actual delay will be a random value in the range {@code [minAsyncDelay, maxAsyncDelay]}.
     *
     * @return minimum delay in seconds, 0 means no delay.
     * @since 2.26.0
     */
    default int getMinAsyncDelay() {
        return 0;
    }

    /**
     * The maximum delay in seconds before the asynchronous action should be executed.
     * A value of 0 means the action should be executed immediately.
     * The actual delay will be a random value in the range {@code [minAsyncDelay, maxAsyncDelay]}.
     *
     * @return maximum delay in seconds, 0 means no delay.
     * @since 2.26.0
     */
    default int getMaxAsyncDelay() {
        return 0;
    }
}

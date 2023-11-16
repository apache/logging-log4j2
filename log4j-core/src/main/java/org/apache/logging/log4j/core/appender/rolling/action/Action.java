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

/**
 * The Action interface should be implemented by any class that performs
 * file system actions for RollingFileAppenders after the close of
 * the active log file.
 */
public interface Action extends Runnable {
    /**
     * Perform an action.
     *
     * @return true if action was successful.  A return value of false will cause
     *         the rollover to be aborted if possible.
     * @throws IOException if IO error, a thrown exception will cause the rollover
     *                     to be aborted if possible.
     */
    boolean execute() throws IOException;

    /**
     * Cancels the action if not already initialized or waits till completion.
     */
    void close();

    /**
     * Determines if action has been completed.
     *
     * @return true if action is complete.
     */
    boolean isComplete();
}

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
package org.apache.logging.log4j.core;

/**
 * Appenders may delegate their error handling to <code>ErrorHandlers</code>.
 * TODO if the appender interface is simplified, then error handling could just be done by wrapping
 *  a nested appender. (RG) Please look at DefaultErrorHandler. It's purpose is to make sure the console
 * or error log isn't flooded with messages. I'm still considering the Appender refactoring.
 */
public interface ErrorHandler {

    /**
     * Handle an error with a message.
     * @param msg The message.
     */
    void error(String msg);

    /**
     * Handle an error with a message and an exception.
     * @param msg The message.
     * @param t The Throwable.
     */
    void error(String msg, Throwable t);

    /**
     * Handle an error with a message, and exception and a logging event.
     * @param msg The message.
     * @param event The LogEvent.
     * @param t The Throwable.
     */
    void error(String msg, LogEvent event, Throwable t);
}

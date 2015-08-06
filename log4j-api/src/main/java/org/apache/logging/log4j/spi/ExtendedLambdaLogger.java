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

package org.apache.logging.log4j.spi;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LambdaLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;

/**
 * Extends the {@code LambdaLogger} interface with methods that facilitate implementing or extending
 * {@code LambdaLogger}s. Users should not need to use this interface.
 */
public interface ExtendedLambdaLogger extends ExtendedLogger, LambdaLogger {

    /**
     * Logs a message whose parameters are only to be constructed if the specified level is active.
     * 
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param message The message format.
     * @param paramSuppliers An array of functions, which when called, produce the desired log message parameters.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Callable<?>... paramSuppliers);

    /**
     * Logs a message which is only to be constructed if the specified level is active.
     * 
     * @param fqcn The fully qualified class name of the logger entry point, used to determine the caller class and
     *            method when location information needs to be logged.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param msgSupplier A function, which when called, produces the desired log message.
     * @param t the exception to log, including its stack trace.
     */
    void logIfEnabled(String fqcn, Level level, Marker marker, Callable<?> msgSupplier, Throwable t);

}

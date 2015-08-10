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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger2;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Extends the {@code Logger2} interface with methods that facilitate implementing or extending
 * {@code Logger2}s. Users should not need to use this interface.
 * 
 * @since 2.4
 */
public interface ExtendedLogger2 extends ExtendedLogger, Logger2 {

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
    void logIfEnabled(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t);

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
    void logIfEnabled(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers);

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
    void logIfEnabled(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t);

}

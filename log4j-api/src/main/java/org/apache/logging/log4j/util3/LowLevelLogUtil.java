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

package org.apache.logging.log4j.util3;

import java.io.PrintWriter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * PrintWriter-based logging utility for classes too low level to use {@link org.apache.logging.log4j.status.StatusLogger}.
 * Such classes cannot use StatusLogger as StatusLogger or {@link org.apache.logging.log4j.simple.SimpleLogger} depends
 * on them for initialization. Other framework classes should stick to using StatusLogger.
 *
 * @since 2.6
 */
public final class LowLevelLogUtil {
    private static final PrintWriter STDERR = new PrintWriter(System.err, true);
    private static Consumer<String> logErrorMessage = message -> STDERR.println("ERROR: " + message);
    private static Consumer<Throwable> logException = exception -> exception.printStackTrace(STDERR);
    private static BiConsumer<String, Throwable> logErrorWithException = (message, exception) -> {
        log(message);
        logException(exception);
    };

    public static void setLogErrorMessage(final Consumer<String> logErrorMessage) {
        LowLevelLogUtil.logErrorMessage = logErrorMessage;
    }

    public static void setLogException(final Consumer<Throwable> logException) {
        LowLevelLogUtil.logException = logException;
    }

    public static void setLogErrorWithException(final BiConsumer<String, Throwable> logErrorWithException) {
        LowLevelLogUtil.logErrorWithException = logErrorWithException;
    }

    /**
     * Logs the given message.
     * 
     * @param message the message to log
     * @since 2.9.2
     */
    public static void log(final String message) {
        if (message != null) {
            logErrorMessage.accept(message);
        }
    }

    public static void logException(final Throwable exception) {
        if (exception != null) {
            logException.accept(exception);
        }
    }

    public static void logException(final String message, final Throwable exception) {
        logErrorWithException.accept(message, exception);
    }

    private LowLevelLogUtil() {
    }
}

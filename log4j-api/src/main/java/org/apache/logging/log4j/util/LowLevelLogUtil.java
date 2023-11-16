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
package org.apache.logging.log4j.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;

/**
 * PrintWriter-based logging utility for classes too low level to use {@link org.apache.logging.log4j.status.StatusLogger}.
 * Such classes cannot use StatusLogger as StatusLogger or {@link org.apache.logging.log4j.simple.SimpleLogger} depends
 * on them for initialization. Other framework classes should stick to using StatusLogger.
 *
 * @since 2.6
 */
final class LowLevelLogUtil {

    /*
     * https://errorprone.info/bugpattern/DefaultCharset
     *
     * We intentionally use the system encoding.
     */
    @SuppressWarnings("DefaultCharset")
    private static PrintWriter writer = new PrintWriter(System.err, true);

    /**
     * Logs the given message.
     *
     * @param message the message to log
     * @since 2.9.2
     */
    public static void log(final String message) {
        if (message != null) {
            writer.println(message);
        }
    }

    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "Log4j prints stacktraces only to logs, which should be private.")
    public static void logException(final Throwable exception) {
        if (exception != null) {
            exception.printStackTrace(writer);
        }
    }

    public static void logException(final String message, final Throwable exception) {
        log(message);
        logException(exception);
    }

    /**
     * Sets the underlying OutputStream where exceptions are printed to.
     *
     * @param out the OutputStream to log to
     */
    @SuppressWarnings("DefaultCharset")
    public static void setOutputStream(final OutputStream out) {
        /*
         * https://errorprone.info/bugpattern/DefaultCharset
         *
         * We intentionally use the system encoding.
         */
        LowLevelLogUtil.writer = new PrintWriter(Objects.requireNonNull(out), true);
    }

    /**
     * Sets the underlying Writer where exceptions are printed to.
     *
     * @param writer the Writer to log to
     */
    public static void setWriter(final Writer writer) {
        LowLevelLogUtil.writer = new PrintWriter(Objects.requireNonNull(writer), true);
    }

    private LowLevelLogUtil() {}
}

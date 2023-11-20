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
package org.apache.logging.log4j.core.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps with Throwable objects.
 */
public final class Throwables {

    private Throwables() {}

    /**
     * Returns the deepest cause of the given {@code throwable}.
     *
     * @param throwable the throwable to navigate
     * @return the deepest throwable or the given throwable
     */
    public static Throwable getRootCause(final Throwable throwable) {

        // Keep a second pointer that slowly walks the causal chain. If the fast
        // pointer ever catches the slower pointer, then there's a loop.
        Throwable slowPointer = throwable;
        boolean advanceSlowPointer = false;

        Throwable parent = throwable;
        Throwable cause;
        while ((cause = parent.getCause()) != null) {
            parent = cause;
            if (parent == slowPointer) {
                throw new IllegalArgumentException("loop in causal chain");
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }
        return parent;
    }

    /**
     * Converts a Throwable stack trace into a List of Strings.
     *
     * @param throwable the Throwable
     * @return a List of Strings
     */
    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "Log4j prints stacktraces only to logs, which should be private.")
    public static List<String> toStringList(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch (final RuntimeException ex) {
            // Ignore any exceptions.
        }
        pw.flush();
        final List<String> lines = new ArrayList<>();
        final LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        try {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (final IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex.toString());
        } finally {
            Closer.closeSilently(reader);
        }
        return lines;
    }

    /**
     * Rethrows a {@link Throwable}.
     *
     * @param t the Throwable to throw.
     * @since 2.1
     */
    public static void rethrow(final Throwable t) {
        Throwables.<RuntimeException>rethrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow0(final Throwable t) throws T {
        throw (T) t;
    }
}

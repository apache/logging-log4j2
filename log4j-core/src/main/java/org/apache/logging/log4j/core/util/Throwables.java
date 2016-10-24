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
package org.apache.logging.log4j.core.util;

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

    private Throwables() {
    }

    /**
     * Returns the deepest cause of the given {@code throwable}.
     *
     * @param throwable the throwable to navigate
     * @return the deepest throwable or the given throwable
     */
    public static Throwable getRootCause(final Throwable throwable) {
        Throwable cause;
        Throwable root = throwable;
        while ((cause = root.getCause()) != null) {
            root = cause;
        }
        return root;
    }

    /**
     * Converts a Throwable stack trace into a List of Strings.
     *
     * @param throwable the Throwable
     * @return a List of Strings
     */
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

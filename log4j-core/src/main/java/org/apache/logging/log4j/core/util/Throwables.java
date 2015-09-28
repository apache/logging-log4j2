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
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps with Throwable objects.
 */
public final class Throwables {

    private Throwables() {
    }

    /**
     * Has no effect on Java 6 and below.
     *
     * @param throwable a Throwable
     * @param suppressedThrowable a suppressed Throwable
     * @see Throwable#addSuppressed(Throwable)
     * @deprecated If compiling on Java 7 and above use {@link Throwable#addSuppressed(Throwable)}.
     *             Marked as deprecated because Java 6 is deprecated. Will be removed in 2.5.
     */
    @Deprecated
    public static void addSuppressed(final Throwable throwable, final Throwable suppressedThrowable) {
        throwable.addSuppressed(suppressedThrowable);
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
     * Has no effect on Java 6 and below.
     *
     * @param throwable a Throwable
     * @return see Java 7's {@link Throwable#getSuppressed()}
     * @see Throwable#getSuppressed()
     * @deprecated If compiling on Java 7 and above use {@link Throwable#getSuppressed()}. Marked as deprecated because
     *             Java 6 is deprecated. Will be removed 2.5.
     */
    @Deprecated
    public static Throwable[] getSuppressed(final Throwable throwable) {
        return throwable.getSuppressed();
    }

    /**
     * Returns true if the getSuppressed method is available.
     * 
     * @return True if getSuppressed is available. As of 2.4, always returns true.
     * @deprecated Will be removed in 2.5. As of 2.4, always returns true.
     */
    @Deprecated
    public static boolean isGetSuppressedAvailable() {
        return true;
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
     * Rethrows a {@link Throwable}, wrapping checked exceptions into an {@link UndeclaredThrowableException}.
     *
     * @param t the Throwable to throw.
     * @throws RuntimeException if {@code t} is a RuntimeException
     * @throws Error if {@code t} is an Error
     * @throws UndeclaredThrowableException if {@code t} is a checked Exception
     * @since 2.1
     */
    public static void rethrow(final Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new UndeclaredThrowableException(t);
    }
}

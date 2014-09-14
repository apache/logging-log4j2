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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Helps with Throwable objects.
 */
public final class Throwables {

    private static final Method ADD_SUPPRESSED;

    private static final Method GET_SUPPRESSED;

    static {
        Method getSuppressed = null, addSuppressed = null;
        final Method[] methods = Throwable.class.getMethods();
        for (final Method method : methods) {
            if (method.getName().equals("getSuppressed")) {
                getSuppressed = method;
            } else if (method.getName().equals("addSuppressed")) {
                addSuppressed = method;
            }
        }
        GET_SUPPRESSED = getSuppressed;
        ADD_SUPPRESSED = addSuppressed;
    }

    /**
     * Has no effect on Java 6 and below.
     *
     * @param throwable a Throwable
     * @param suppressedThrowable a suppressed Throwable
     * @see Throwable#addSuppressed(Throwable)
     * @deprecated If compiling on Java 7 and above use {@link Throwable#addSuppressed(Throwable)}. Marked as deprecated because Java 6 is
     *             deprecated.
     */
    @Deprecated
    public static void addSuppressed(final Throwable throwable, final Throwable suppressedThrowable) {
        if (ADD_SUPPRESSED != null) {
            try {
                ADD_SUPPRESSED.invoke(throwable, suppressedThrowable);
            } catch (final IllegalAccessException e) {
                // Only happens on Java >= 7 if this class has a bug.
                StatusLogger.getLogger().error(e);
            } catch (final IllegalArgumentException e) {
                // Only happens on Java >= 7 if this class has a bug.
                StatusLogger.getLogger().error(e);
            } catch (final InvocationTargetException e) {
                // Only happens on Java >= 7 if this class has a bug.
                StatusLogger.getLogger().error(e);
            }
        }

    }

    /**
     * Has no effect on Java 6 and below.
     *
     * @param throwable a Throwable
     * @return see Java 7's {@link Throwable#getSuppressed()}
     * @see Throwable#getSuppressed()
     * @deprecated If compiling on Java 7 and above use {@link Throwable#getSuppressed()}. Marked as deprecated because Java 6 is
     *             deprecated.
     */
    @Deprecated
    public static Throwable[] getSuppressed(final Throwable throwable) {
        if (GET_SUPPRESSED != null) {
            try {
                return (Throwable[]) GET_SUPPRESSED.invoke(throwable);
            } catch (final Exception e) {
                // Only happens on Java >= 7 if this class has a bug.
                StatusLogger.getLogger().error(e);
                return null;
            }
        }
        return null;
    }

    /**
     * Returns true if the getSuppressed method is available.
     * 
     * @return True if getSuppressed is available.
     */
    public static boolean isGetSuppressedAvailable() {
        return GET_SUPPRESSED != null;
    }

    /**
     * Converts a Throwable stack trace into a List of Strings
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
        final List<String> lines = new ArrayList<String>();
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
     * @throws RuntimeException             if {@code t} is a RuntimeException
     * @throws Error                        if {@code t} is an Error
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

    private Throwables() {
    }

}

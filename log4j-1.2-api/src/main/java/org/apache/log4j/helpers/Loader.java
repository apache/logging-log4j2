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
package org.apache.log4j.helpers;

import java.net.URL;

/**
 * Loads resources (or images) from various sources.
 */
public class Loader {

    static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";

    private static boolean ignoreTCL;

    static {
        final String ignoreTCLProp = OptionConverter.getSystemProperty("log4j.ignoreTCL", null);
        if (ignoreTCLProp != null) {
            ignoreTCL = OptionConverter.toBoolean(ignoreTCLProp, true);
        }
    }

    /**
     * This method will search for <code>resource</code> in different places. The search order is as follows:
     * <ol>
     * <p>
     * <li>Search for <code>resource</code> using the thread context class loader under Java2. If that fails, search for
     * <code>resource</code> using the class loader that loaded this class (<code>Loader</code>).
     * </p>
     * <p>
     * <li>Try one last time with <code>ClassLoader.getSystemResource(resource)</code>.
     * </p>
     * </ol>
     */
    public static URL getResource(final String resource) {
        ClassLoader classLoader = null;
        URL url = null;

        try {
            if (!ignoreTCL) {
                classLoader = getTCL();
                if (classLoader != null) {
                    LogLog.debug("Trying to find [" + resource + "] using context classloader " + classLoader + ".");
                    url = classLoader.getResource(resource);
                    if (url != null) {
                        return url;
                    }
                }
            }

            // We could not find resource. Ler us now try with the
            // ClassLoader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LogLog.debug("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        } catch (final Throwable t) {
            // can't be InterruptedException or InterruptedIOException
            // since not declared, must be error or RuntimeError.
            LogLog.warn(TSTR, t);
        }

        // Last ditch attempt: get the resource from the class path. It
        // may be the case that clazz was loaded by the Extentsion class
        // loader which the parent of the system class loader. Hence the
        // code below.
        LogLog.debug("Trying to find [" + resource + "] using ClassLoader.getSystemResource().");
        return ClassLoader.getSystemResource(resource);
    }

    /**
     * Gets a resource by delegating to getResource(String).
     *
     * @param resource resource name
     * @param clazz class, ignored.
     * @return URL to resource or null.
     * @deprecated as of 1.2.
     */
    @Deprecated
    public static URL getResource(final String resource, final Class clazz) {
        return getResource(resource);
    }

    /**
     * Shorthand for {@code Thread.currentThread().getContextClassLoader()}.
     */
    private static ClassLoader getTCL() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Always returns false since Java 1.x support is long gone.
     *
     * @return Always false.
     */
    public static boolean isJava1() {
        return false;
    }

    /**
     * Loads the specified class using the <code>Thread</code> <code>contextClassLoader</code>, if that fails try
     * Class.forname.
     *
     * @param clazz The class to load.
     * @return The Class.
     * @throws ClassNotFoundException Never thrown, declared for compatibility.
     */
    public static Class loadClass(final String clazz) throws ClassNotFoundException {
        // Just call Class.forName(clazz) if we are instructed to ignore the TCL.
        if (ignoreTCL) {
            return Class.forName(clazz);
        }
        try {
            return getTCL().loadClass(clazz);
        } catch (final Throwable t) {
            // ignore
        }
        return Class.forName(clazz);
    }
}

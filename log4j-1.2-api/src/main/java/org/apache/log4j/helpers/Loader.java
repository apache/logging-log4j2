/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Loads resources (or images) from various sources.
 */
public class Loader {

    static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";

    // We conservatively assume that we are running under Java 1.x
    static private boolean java1 = true;

    static private boolean ignoreTCL = false;

    static {
        String prop = OptionConverter.getSystemProperty("java.version", null);

        if (prop != null) {
            int i = prop.indexOf('.');
            if (i != -1) {
                if (prop.charAt(i + 1) != '1')
                    java1 = false;
            }
        }
        String ignoreTCLProp = OptionConverter.getSystemProperty("log4j.ignoreTCL", null);
        if (ignoreTCLProp != null) {
            ignoreTCL = OptionConverter.toBoolean(ignoreTCLProp, true);
        }
    }

    /**
     * This method will search for <code>resource</code> in different places. The search order is as follows:
     * <ol>
     * <p>
     * <li>Search for <code>resource</code> using the thread context class loader under Java2. If that fails, search for
     * <code>resource</code> using the class loader that loaded this class (<code>Loader</code>). Under JDK 1.1, only the
     * the class loader that loaded this class (<code>Loader</code>) is used.
     * </p>
     * <p>
     * <li>Try one last time with <code>ClassLoader.getSystemResource(resource)</code>, that is is using the system class
     * loader in JDK 1.2 and virtual machine's built-in class loader in JDK 1.1.
     * </p>
     * </ol>
     */
    static public URL getResource(String resource) {
        ClassLoader classLoader = null;
        URL url = null;

        try {
            if (!java1 && !ignoreTCL) {
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
            // classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LogLog.debug("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        } catch (IllegalAccessException t) {
            LogLog.warn(TSTR, t);
        } catch (InvocationTargetException t) {
            if (t.getTargetException() instanceof InterruptedException || t.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LogLog.warn(TSTR, t);
        } catch (Throwable t) {
            //
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
     * Get a resource by delegating to getResource(String).
     * 
     * @param resource resource name
     * @param clazz class, ignored.
     * @return URL to resource or null.
     * @deprecated as of 1.2.
     */
    @Deprecated
    public static URL getResource(String resource, Class clazz) {
        return getResource(resource);
    }

    /**
     * Get the Thread Context Loader which is a JDK 1.2 feature. If we are running under JDK 1.1 or anything else goes wrong
     * the method returns <code>null<code>.
     *
     */
    private static ClassLoader getTCL() throws IllegalAccessException, InvocationTargetException {

        // Are we running on a JDK 1.2 or later system?
        Method method = null;
        try {
            method = Thread.class.getMethod("getContextClassLoader", (Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            // We are running on JDK 1.1
            return null;
        }

        return (ClassLoader) method.invoke(Thread.currentThread(), (Object[]) null);
    }

    /**
     * Are we running under JDK 1.x?
     */
    public static boolean isJava1() {
        return java1;
    }

    /**
     * If running under JDK 1.2 load the specified class using the <code>Thread</code> <code>contextClassLoader</code> if
     * that fails try Class.forname. Under JDK 1.1 only Class.forName is used.
     *
     */
    static public Class loadClass(String clazz) throws ClassNotFoundException {
        // Just call Class.forName(clazz) if we are running under JDK 1.1
        // or if we are instructed to ignore the TCL.
        if (java1 || ignoreTCL) {
            return Class.forName(clazz);
        }
        try {
            return getTCL().loadClass(clazz);
        }
        // we reached here because tcl was null or because of a
        // security exception, or because clazz could not be loaded...
        // In any case we now try one more time
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof InterruptedException || e.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable t) {
            // ignore
        }
        return Class.forName(clazz);
    }
}

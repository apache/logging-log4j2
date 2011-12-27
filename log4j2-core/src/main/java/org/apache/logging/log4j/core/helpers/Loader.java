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

package org.apache.logging.log4j.core.helpers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * Load resources (or images) from various sources.
 */

public class Loader {

    static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";

    static private boolean ignoreTCL = false;

    static Logger logger = StatusLogger.getLogger();

    static {
        String ignoreTCLProp = OptionConverter.getSystemProperty("log4j.ignoreTCL", null);
        if (ignoreTCLProp != null) {
            ignoreTCL = OptionConverter.toBoolean(ignoreTCLProp, true);
        }
    }

    /**
     * This method will search for <code>resource</code> in different
     * places. The search order is as follows:
     * <p/>
     * <ol>
     * <p/>
     * <p><li>Search for <code>resource</code> using the thread context
     * class loader under Java2. If that fails, search for
     * <code>resource</code> using the class loader that loaded this
     * class (<code>Loader</code>). Under JDK 1.1, only the the class
     * loader that loaded this class (<code>Loader</code>) is used.
     * <p/>
     * <p><li>Try one last time with
     * <code>ClassLoader.getSystemResource(resource)</code>, that is is
     * using the system class loader in JDK 1.2 and virtual machine's
     * built-in class loader in JDK 1.1.
     * <p/>
     * </ol>
     * @param resource The resource to load.
     * @return A URL to the resource.
     */
    public static URL getResource(String resource) {
        ClassLoader classLoader;
        URL url;

        try {
            classLoader = getTCL();
            if (classLoader != null) {
                logger.trace("Trying to find [" + resource + "] using context classloader "
                        + classLoader + ".");
                url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                logger.trace("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        } catch (IllegalAccessException t) {
            logger.warn(TSTR, t);
        } catch (InvocationTargetException t) {
            if (t.getTargetException() instanceof InterruptedException
                || t.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            logger.warn(TSTR, t);
        } catch (Throwable t) {
            //
            //  can't be InterruptedException or InterruptedIOException
            //    since not declared, must be error or RuntimeError.
            logger.warn(TSTR, t);
        }

        // Last ditch attempt: get the resource from the class path. It
        // may be the case that clazz was loaded by the Extentsion class
        // loader which the parent of the system class loader. Hence the
        // code below.
        logger.trace("Trying to find [" + resource + "] using ClassLoader.getSystemResource().");
        return ClassLoader.getSystemResource(resource);
    }

    /**
     * This method will search for <code>resource</code> in different
     * places. The search order is as follows:
     * <p/>
     * <ol>
     * <p/>
     * <p><li>Search for <code>resource</code> using the thread context
     * class loader under Java2. If that fails, search for
     * <code>resource</code> using the class loader that loaded this
     * class (<code>Loader</code>). Under JDK 1.1, only the the class
     * loader that loaded this class (<code>Loader</code>) is used.
     * <p/>
     * <p><li>Try one last time with
     * <code>ClassLoader.getSystemResource(resource)</code>, that is is
     * using the system class loader in JDK 1.2 and virtual machine's
     * built-in class loader in JDK 1.1.
     * <p/>
     * </ol>
     * @param resource The resource to load.
     * @param defaultLoader The default ClassLoader.
     * @return An InputStream to read the resouce.
     */
    public static InputStream getResourceAsStream(String resource, ClassLoader defaultLoader) {
        ClassLoader classLoader;
        InputStream is;

        try {
            classLoader = getTCL();
            if (classLoader != null) {
                logger.trace("Trying to find [" + resource + "] using context classloader " + classLoader + ".");
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                logger.trace("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Finally try with the default ClassLoader.
            if (defaultLoader != null) {
                logger.trace("Trying to find [" + resource + "] using " + defaultLoader + " class loader.");
                is = defaultLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }
        } catch (IllegalAccessException t) {
            logger.warn(TSTR, t);
        } catch (InvocationTargetException t) {
            if (t.getTargetException() instanceof InterruptedException
                || t.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            logger.warn(TSTR, t);
        } catch (Throwable t) {
            //
            //  can't be InterruptedException or InterruptedIOException
            //    since not declared, must be error or RuntimeError.
            logger.warn(TSTR, t);
        }

        // Last ditch attempt: get the resource from the class path. It
        // may be the case that clazz was loaded by the Extentsion class
        // loader which the parent of the system class loader. Hence the
        // code below.
        logger.trace("Trying to find [" + resource + "] using ClassLoader.getSystemResource().");
        return ClassLoader.getSystemResourceAsStream(resource);
    }

    public static Class loadClass(String clazz) throws ClassNotFoundException {
        // Just call Class.forName(clazz) if we are instructed to ignore the TCL.
        if (ignoreTCL) {
            return Class.forName(clazz);
        } else {
            try {
                return getTCL().loadClass(clazz);
            }
            catch (Throwable e) {
                return Class.forName(clazz);
            }
        }
    }

    public static ClassLoader getClassLoader() {
        ClassLoader cl = null;

        try {
            cl = getTCL();
        } catch (Exception ex) {
            // Ignore the exception. The ClassLoader will be located.
        }
        if (cl == null) {
            cl = Loader.getClassLoader();
        }
        return cl;
    }

    private static ClassLoader getTCL() throws IllegalAccessException, InvocationTargetException {
        ClassLoader cl;
        if (System.getSecurityManager() == null) {
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            cl = (ClassLoader) java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
        }

        return cl;
    }
}

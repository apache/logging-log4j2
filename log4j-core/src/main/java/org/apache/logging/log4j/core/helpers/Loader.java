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
package org.apache.logging.log4j.core.helpers;

import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Load resources (or images) from various sources.
 */
public final class Loader {

    private static boolean ignoreTCL = false;

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";

    static {
        final String ignoreTCLProp = PropertiesUtil.getProperties().getStringProperty("log4j.ignoreTCL", null);
        if (ignoreTCLProp != null) {
            ignoreTCL = OptionConverter.toBoolean(ignoreTCLProp, true);
        }
    }

    /**
     * Returns the ClassLoader to use.
     * @return the ClassLoader.
     */
    public static ClassLoader getClassLoader() {

        return getClassLoader(Loader.class, null);
    }

    public static ClassLoader getClassLoader(final Class<?> class1, final Class<?> class2) {

        ClassLoader loader1 = null;
        try {
            loader1 = getTCL();
        } catch (final Exception ex) {
            LOGGER.warn("Caught exception locating thread ClassLoader {}", ex.getMessage());
        }
        final ClassLoader loader2 = class1 == null ? null : class1.getClassLoader();
        final ClassLoader loader3 = class2 == null ? null : class2.getClass().getClassLoader();

        if (isChild(loader1, loader2)) {
            return isChild(loader1, loader3) ? loader1 : loader3;
        }
        return isChild(loader2, loader3) ? loader2 : loader3;
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
     * @return A URL to the resource.
     */
    public static URL getResource(final String resource, final ClassLoader defaultLoader) {
        try {
            ClassLoader classLoader = getTCL();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using context classloader "
                        + classLoader + '.');
                final URL url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                final URL url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
            // We could not find resource. Finally try with the default ClassLoader.
            if (defaultLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using " + defaultLoader + " class loader.");
                final URL url = defaultLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        } catch (final Throwable t) {
            //
            //  can't be InterruptedException or InterruptedIOException
            //    since not declared, must be error or RuntimeError.
            LOGGER.warn(TSTR, t);
        }

        // Last ditch attempt: get the resource from the class path. It
        // may be the case that clazz was loaded by the Extension class
        // loader which the parent of the system class loader. Hence the
        // code below.
        LOGGER.trace("Trying to find [" + resource + "] using ClassLoader.getSystemResource().");
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
    public static InputStream getResourceAsStream(final String resource, final ClassLoader defaultLoader) {
        ClassLoader classLoader;
        InputStream is;

        try {
            classLoader = getTCL();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using context classloader " + classLoader + '.');
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using " + classLoader + " class loader.");
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Finally try with the default ClassLoader.
            if (defaultLoader != null) {
                LOGGER.trace("Trying to find [" + resource + "] using " + defaultLoader + " class loader.");
                is = defaultLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }
        } catch (final Throwable t) {
            //
            //  can't be InterruptedException or InterruptedIOException
            //    since not declared, must be error or RuntimeError.
            LOGGER.warn(TSTR, t);
        }

        // Last ditch attempt: get the resource from the class path. It
        // may be the case that clazz was loaded by the Extension class
        // loader which the parent of the system class loader. Hence the
        // code below.
        LOGGER.trace("Trying to find [" + resource + "] using ClassLoader.getSystemResource().");
        return ClassLoader.getSystemResourceAsStream(resource);
    }

    private static ClassLoader getTCL() {
        ClassLoader cl;
        if (System.getSecurityManager() == null) {
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            cl = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
        }

        return cl;
    }

    private static boolean isChild(final ClassLoader loader1, final ClassLoader loader2) {
        if (loader1 != null && loader2 != null) {
            ClassLoader parent = loader1.getParent();
            while (parent != null && parent != loader2) {
                parent = parent.getParent();
            }
            return parent != null;
        }
        return loader1 != null;
    }

    /**
     * Load a Class by name.
     * @param className The class name.
     * @return The Class.
     * @throws ClassNotFoundException if the Class could not be found.
     */
    public static Class<?> loadClass(final String className) throws ClassNotFoundException {
        // Just call Class.forName(className) if we are instructed to ignore the TCL.
        if (ignoreTCL) {
            return Class.forName(className);
        }
        try {
            return getTCL().loadClass(className);
        } catch (final Throwable e) {
            return Class.forName(className);
        }
    }

    private Loader() {
    }
}

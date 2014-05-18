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


import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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
     * This method will search for {@code resource} in different
     * places. The search order is as follows:
     * <p/>
     * <ol>
     * <p/>
     * <p><li>Search for {@code resource} using the thread context
     * class loader under Java2. If that fails, search for
     * {@code resource} using the class loader that loaded this
     * class ({@code Loader}). Under JDK 1.1, only the the class
     * loader that loaded this class ({@code Loader}) is used.
     * <p/>
     * <p><li>Try one last time with
     * {@code ClassLoader.getSystemResource(resource)}, that is is
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
                LOGGER.trace("Trying to find [{}] using context class loader {}.", resource, classLoader);
                final URL url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [{}] using {} class loader.", resource, classLoader);
                final URL url = classLoader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
            // We could not find resource. Finally try with the default ClassLoader.
            if (defaultLoader != null) {
                LOGGER.trace("Trying to find [{}] using {} class loader.", resource, defaultLoader);
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
        LOGGER.trace("Trying to find [{}] using ClassLoader.getSystemResource().", resource);
        return ClassLoader.getSystemResource(resource);
    }

    /**
     * This method will search for {@code resource} in different
     * places. The search order is as follows:
     * <p/>
     * <ol>
     * <p/>
     * <p><li>Search for {@code resource} using the thread context
     * class loader under Java2. If that fails, search for
     * {@code resource} using the class loader that loaded this
     * class ({@code Loader}). Under JDK 1.1, only the the class
     * loader that loaded this class ({@code Loader}) is used.
     * <p/>
     * <p><li>Try one last time with
     * {@code ClassLoader.getSystemResource(resource)}, that is is
     * using the system class loader in JDK 1.2 and virtual machine's
     * built-in class loader in JDK 1.1.
     * <p/>
     * </ol>
     * @param resource The resource to load.
     * @param defaultLoader The default ClassLoader.
     * @return An InputStream to read the resouce.
     */
    public static InputStream getResourceAsStream(final String resource, final ClassLoader defaultLoader) {
        try {
            ClassLoader classLoader = getTCL();
            InputStream is;
            if (classLoader != null) {
                LOGGER.trace("Trying to find [{}] using context class loader {}.", resource, classLoader);
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Let us now try with the classloader that loaded this class.
            classLoader = Loader.class.getClassLoader();
            if (classLoader != null) {
                LOGGER.trace("Trying to find [{}] using {} class loader.", resource, classLoader);
                is = classLoader.getResourceAsStream(resource);
                if (is != null) {
                    return is;
                }
            }

            // We could not find resource. Finally try with the default ClassLoader.
            if (defaultLoader != null) {
                LOGGER.trace("Trying to find [{}] using {} class loader.", resource, defaultLoader);
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
        LOGGER.trace("Trying to find [{}] using ClassLoader.getSystemResource().", resource);
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
     * Load a Class by name. Note that unlike {@link ClassLoader#loadClass(String) ClassLoader.loadClass}, this method
     * will initialize the class as well if it hasn't been already. This is equivalent to the calling the
     * {@link ClassLoader#loadClass(String, boolean) protected version} with the second parameter equal to {@code true}.
     *
     * @param className The class name.
     * @return The Class.
     * @throws ClassNotFoundException if the Class could not be found.
     */
    public static Class<?> loadClass(final String className) throws ClassNotFoundException {
        // Just call Class.forName(className) if we are instructed to ignore the TCL.
        if (ignoreTCL) {
            LOGGER.trace("Ignoring TCCL. Trying Class.forName({}).", className);
            return loadClassWithDefaultClassLoader(className);
        }
        try {
            LOGGER.trace("Trying TCCL for class {}.", className);
            // using the TCCL should work the same as the default ClassLoader (i.e., init or not)
            return Class.forName(className, true, getTCL());
        } catch (final Throwable e) {
            LOGGER.trace("TCCL didn't work. Trying Class.forName({}).", className, e);
            return loadClassWithDefaultClassLoader(className);
        }
    }

    private static Class<?> loadClassWithDefaultClassLoader(final String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    /**
     * Loads and initializes a named Class using a given ClassLoader.
     *
     * @param className The class name.
     * @param loader The class loader.
     * @return The class.
     * @throws ClassNotFoundException if the class could not be found.
     */
    public static Class<?> initializeClass(final String className, final ClassLoader loader)
            throws ClassNotFoundException {
        return Class.forName(className, true, loader);
    }

    /**
     * Load a Class in the {@code java.*} namespace by name. Useful for peculiar scenarios typically involving
     * Google App Engine.
     *
     * @param className The class name.
     * @return The Class.
     * @throws ClassNotFoundException if the Class could not be found.
     */
    public static Class<?> loadSystemClass(final String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, ClassLoader.getSystemClassLoader());
        } catch (final Throwable t) {
            LOGGER.trace("Couldn't use SystemClassLoader. Trying Class.forName({}).", className, t);
            return Class.forName(className);
        }
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param className The class name.
     * @return new instance of the class.
     * @throws ClassNotFoundException if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException if the class can't be instantiated through a public constructor
     * @throws InstantiationException if there was an exception whilst instantiating the class
     * @throws NoSuchMethodException if there isn't a no-args constructor on the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     */
    public static Object newInstanceOf(final String className)
            throws ClassNotFoundException,
                   IllegalAccessException,
                   InstantiationException,
                   NoSuchMethodException,
                   InvocationTargetException {
        final Class<?> clazz = loadClass(className);
        try {
            return clazz.getConstructor().newInstance();
        } catch (final NoSuchMethodException e) {
            // try the default-default constructor
            //noinspection ClassNewInstance
            return clazz.newInstance();
        }
    }

    /**
     * Loads, instantiates, and casts a Class using the default constructor.
     *
     * @param className The class name.
     * @param clazz The class to cast it to.
     * @param <T> The type to cast it to.
     * @return new instance of the class cast to {@code T}
     * @throws ClassNotFoundException if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException if the class can't be instantiated through a public constructor
     * @throws InstantiationException if there was an exception whilst instantiating the class
     * @throws NoSuchMethodException if there isn't a no-args constructor on the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @throws ClassCastException if the constructed object isn't type compatible with {@code T}
     */
    public static <T> T newCheckedInstanceOf(final String className, final Class<T> clazz)
            throws ClassNotFoundException,
                   NoSuchMethodException,
                   IllegalAccessException,
                   InvocationTargetException,
                   InstantiationException {
        return clazz.cast(newInstanceOf(className));
    }

    /**
     * Determines if a named Class can be loaded or not.
     *
     * @param className The class name.
     * @return {@code true} if the class could be found or {@code false} otherwise.
     */
    public static boolean isClassAvailable(final String className) {
        try {
            final Class<?> clazz = loadClass(className);
            return clazz != null;
        } catch (final ClassNotFoundException e) {
            return false;
        } catch (final Throwable e) {
            LOGGER.trace("Unknown error checking for existence of class [{}].", className, e);
            return false;
        }
    }

    private Loader() {
    }
}

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
package org.apache.logging.log4j.util;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * <em>Consider this class private.</em> Utility class for ClassLoaders.
 * @see ClassLoader
 * @see Thread#getContextClassLoader()
 */
// TODO: migrate any other useful methods from Loader in log4j-core
public final class LoaderUtil {
    private LoaderUtil() {}

    public static final String IGNORE_TCCL_PROPERTY = "log4j.ignoreTCL";

    private static final boolean IGNORE_TCCL;

    private static final PrivilegedAction<ClassLoader> TCCL_GETTER = new ThreadContextClassLoaderGetter();

    static {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        }
        final String ignoreTccl = PropertiesUtil.getProperties().getStringProperty(IGNORE_TCCL_PROPERTY, null);
        IGNORE_TCCL = ignoreTccl != null && !"false".equalsIgnoreCase(ignoreTccl.trim());
    }

    /**
     * Gets the current Thread ClassLoader. Returns the system ClassLoader if the TCCL is {@code null}.
     *
     * @return the current ThreadContextClassLoader.
     */
    public static ClassLoader getThreadContextClassLoader() {
        return System.getSecurityManager() == null
            ? TCCL_GETTER.run()
            : AccessController.doPrivileged(TCCL_GETTER);
    }

    private static class ThreadContextClassLoaderGetter implements PrivilegedAction<ClassLoader> {
        @Override
        public ClassLoader run() {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            // if the TCCL is null, that means we're using the system CL
            return cl == null ? ClassLoader.getSystemClassLoader() : cl;
        }
    }

    /**
     * Loads a class by name. This method respects the {@link #IGNORE_TCCL_PROPERTY} Log4j property. If this property
     * is specified and set to anything besides {@code false}, then the default ClassLoader will be used.
     *
     * @param className The class name.
     * @return the Class for the given name.
     * @throws ClassNotFoundException if the specified class name could not be found
     */
    public static Class<?> loadClass(final String className) throws ClassNotFoundException {
        if (IGNORE_TCCL) {
            return Class.forName(className);
        }
        try {
            return getThreadContextClassLoader().loadClass(className);
        } catch (final Throwable e) {
            return Class.forName(className);
        }
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param className The class name.
     * @return new instance of the class.
     * @throws ClassNotFoundException    if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException    if the class can't be instantiated through a public constructor
     * @throws InstantiationException    if there was an exception whilst instantiating the class
     * @throws NoSuchMethodException     if there isn't a no-args constructor on the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     */
    public static Object newInstanceOf(final String className)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException,
        InvocationTargetException {
        final Class<?> clazz = loadClass(className);
        try {
            return clazz.getConstructor().newInstance();
        } catch (final NoSuchMethodException e) {
            return clazz.newInstance();
        }
    }

    /**
     * Loads and instantiates a derived class using its default constructor.
     *
     * @param className The class name.
     * @param clazz The class to cast it to.
     * @param <T> The type of the class to check.
     * @return new instance of the class cast to {@code T}
     * @throws ClassNotFoundException if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException if the class can't be instantiated through a public constructor
     * @throws InstantiationException if there was an exception whilst instantiating the class
     * @throws NoSuchMethodException if there isn't a no-args constructor on the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @throws ClassCastException if the constructed object isn't type compatible with {@code T}
     */
    public static <T> T newCheckedInstanceOf(final String className, final Class<T> clazz)
        throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
        IllegalAccessException {
        return clazz.cast(newInstanceOf(className));
    }
}

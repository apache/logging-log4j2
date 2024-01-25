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
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em> Utility class for ClassLoaders.
 *
 * @see ClassLoader
 * @see RuntimePermission
 * @see Thread#getContextClassLoader()
 * @see ClassLoader#getSystemClassLoader()
 */
@InternalApi
public final class LoaderUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * System property to set to ignore the thread context ClassLoader.
     *
     * @since 2.1
     */
    public static final String IGNORE_TCCL_PROPERTY = "log4j.ignoreTCL";

    // this variable must be lazily loaded; otherwise, we get a nice circular class loading problem where LoaderUtil
    // wants to use PropertiesUtil, but then PropertiesUtil wants to use LoaderUtil.
    private static Boolean ignoreTCCL;

    static final RuntimePermission GET_CLASS_LOADER = new RuntimePermission("getClassLoader");
    static final LazyBoolean GET_CLASS_LOADER_DISABLED = new LazyBoolean(() -> {
        if (System.getSecurityManager() == null) {
            return false;
        }
        try {
            AccessController.checkPermission(GET_CLASS_LOADER);
            // seems like we'll be ok
            return false;
        } catch (final SecurityException ignored) {
            try {
                // let's see if we can obtain that permission
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    AccessController.checkPermission(GET_CLASS_LOADER);
                    return null;
                });
                return false;
            } catch (final SecurityException ignore) {
                // no chance
                return true;
            }
        }
    });

    private static final PrivilegedAction<ClassLoader> TCCL_GETTER = new ThreadContextClassLoaderGetter();

    private LoaderUtil() {}

    /**
     * Returns the ClassLoader to use.
     *
     * @return the ClassLoader.
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(LoaderUtil.class, null);
    }

    // TODO: this method could use some explanation
    public static ClassLoader getClassLoader(final Class<?> class1, final Class<?> class2) {
        PrivilegedAction<ClassLoader> action = () -> {
            final ClassLoader loader1 = class1 == null ? null : class1.getClassLoader();
            final ClassLoader loader2 = class2 == null ? null : class2.getClassLoader();
            final ClassLoader referenceLoader = GET_CLASS_LOADER_DISABLED.getAsBoolean()
                    ? getThisClassLoader()
                    : Thread.currentThread().getContextClassLoader();
            if (isChild(referenceLoader, loader1)) {
                return isChild(referenceLoader, loader2) ? referenceLoader : loader2;
            }
            return isChild(loader1, loader2) ? loader1 : loader2;
        };
        return runPrivileged(action);
    }

    /**
     * Determines if one ClassLoader is a child of another ClassLoader. Note that a {@code null} ClassLoader is
     * interpreted as the system ClassLoader as per convention.
     *
     * @param loader1 the ClassLoader to check for childhood.
     * @param loader2 the ClassLoader to check for parenthood.
     * @return {@code true} if the first ClassLoader is a strict descendant of the second ClassLoader.
     */
    private static boolean isChild(final ClassLoader loader1, final ClassLoader loader2) {
        if (loader1 != null && loader2 != null) {
            ClassLoader parent = loader1.getParent();
            while (parent != null && parent != loader2) {
                parent = parent.getParent();
            }
            // once parent is null, we're at the system CL, which would indicate they have separate ancestry
            return parent != null;
        }
        return loader1 != null;
    }

    /**
     * Looks up the ClassLoader for this current thread. If this class does not have the runtime permission
     * {@code getClassLoader}, then the only ClassLoader this attempts to look up is the loader behind this
     * class. When a SecurityManager is installed, this attempts to make a privileged call to get the current
     * {@linkplain Thread#getContextClassLoader() thread context ClassLoader}, falling back to either the
     * ClassLoader of this class or the {@linkplain ClassLoader#getSystemClassLoader() system ClassLoader}.
     * When no SecurityManager is present, the same lookups are performed without use of {@link AccessController}.
     * If none of these strategies can obtain a ClassLoader, then this returns {@code null}.
     *
     * @return the current thread's ClassLoader, a fallback loader, or null if no fallback can be determined
     */
    public static ClassLoader getThreadContextClassLoader() {
        try {
            return GET_CLASS_LOADER_DISABLED.getAsBoolean() ? getThisClassLoader() : runPrivileged(TCCL_GETTER);
        } catch (final SecurityException ignored) {
            return null;
        }
    }

    private static ClassLoader getThisClassLoader() {
        return LoaderUtil.class.getClassLoader();
    }

    private static <T> T runPrivileged(final PrivilegedAction<T> action) {
        return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
    }

    private static class ThreadContextClassLoaderGetter implements PrivilegedAction<ClassLoader> {
        @Override
        public ClassLoader run() {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                return contextClassLoader;
            }
            final ClassLoader thisClassLoader = getThisClassLoader();
            if (thisClassLoader != null || GET_CLASS_LOADER_DISABLED.getAsBoolean()) {
                return thisClassLoader;
            }
            return ClassLoader.getSystemClassLoader();
        }
    }

    /**
     * Determines if a named Class can be loaded or not.
     *
     * @param className The class name.
     * @return {@code true} if the class could be found or {@code false} otherwise.
     * @since 2.7
     */
    public static boolean isClassAvailable(final String className) {
        try {
            loadClass(className);
            return true;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        } catch (final Throwable error) {
            LOGGER.error("Unknown error while checking existence of class `{}`", className, error);
            return false;
        }
    }

    /**
     * Loads a class by name. This method respects the {@link #IGNORE_TCCL_PROPERTY} Log4j property. If this property is
     * specified and set to anything besides {@code false}, then the default ClassLoader will be used.
     *
     * @param className fully qualified class name to load
     * @return the loaded class
     * @throws ClassNotFoundException      if the specified class name could not be found
     * @throws ExceptionInInitializerError if an exception is thrown during class initialization
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @since 2.1
     */
    public static Class<?> loadClass(final String className) throws ClassNotFoundException {
        ClassLoader classLoader = isIgnoreTccl() ? getThisClassLoader() : getThreadContextClassLoader();
        if (classLoader == null) {
            classLoader = getThisClassLoader();
        }
        return Class.forName(className, true, classLoader);
    }

    /**
     * Loads and initializes a class given its fully qualified class name. All checked reflective operation
     * exceptions are translated into equivalent {@link LinkageError} classes.
     *
     * @param className fully qualified class name to load
     * @return the loaded class
     * @throws NoClassDefFoundError        if the specified class name could not be found
     * @throws ExceptionInInitializerError if an exception is thrown during class initialization
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @see #loadClass(String)
     * @since 2.22.0
     */
    public static Class<?> loadClassUnchecked(final String className) {
        try {
            return loadClass(className);
        } catch (final ClassNotFoundException e) {
            final NoClassDefFoundError error = new NoClassDefFoundError(e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param <T>   the type of the class modeled by the {@code Class} object.
     * @param clazz The class.
     * @return new instance of the class.
     * @throws NoSuchMethodException       if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws IllegalAccessException      if the class can't be instantiated through a public constructor
     * @throws InstantiationException      if the provided class is abstract or an interface
     * @throws InvocationTargetException   if an exception is thrown by the constructor
     * @throws ExceptionInInitializerError if an exception was thrown while initializing the class
     * @since 2.7
     */
    public static <T> T newInstanceOf(final Class<T> clazz)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Constructor<T> constructor = clazz.getDeclaredConstructor();
        return constructor.newInstance();
    }

    /**
     * Creates an instance of the provided class using the default constructor. All checked reflective operation
     * exceptions are translated into {@link LinkageError} or {@link InternalException}.
     *
     * @param clazz class to instantiate
     * @param <T>   the type of the object being instantiated
     * @return instance of the class
     * @throws NoSuchMethodError  if no zero-arg constructor exists
     * @throws SecurityException  if this class is not allowed to access declared members of the provided class
     * @throws InternalException  if an exception is thrown by the constructor
     * @throws InstantiationError if the provided class is abstract or an interface
     * @throws IllegalAccessError if the class cannot be accessed
     * @since 2.22.0
     */
    public static <T> T newInstanceOfUnchecked(final Class<T> clazz) {
        try {
            return newInstanceOf(clazz);
        } catch (final NoSuchMethodException e) {
            final NoSuchMethodError error = new NoSuchMethodError(e.getMessage());
            error.initCause(e);
            throw error;
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            throw new InternalException(cause);
        } catch (final InstantiationException e) {
            final InstantiationError error = new InstantiationError(e.getMessage());
            error.initCause(e);
            throw error;
        } catch (final IllegalAccessException e) {
            final IllegalAccessError error = new IllegalAccessError(e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param className fully qualified class name to load, initialize, and construct
     * @param <T>       type the class must be compatible with
     * @return new instance of the class
     * @throws ClassNotFoundException      if the class isn't available to the usual ClassLoaders
     * @throws ExceptionInInitializerError if an exception was thrown while initializing the class
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @throws ClassCastException          if the class is not compatible with the generic type parameter provided
     * @throws NoSuchMethodException       if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws IllegalAccessException      if the class can't be instantiated through a public constructor
     * @throws InstantiationException      if the provided class is abstract or an interface
     * @throws InvocationTargetException   if an exception is thrown by the constructor
     * @since 2.1
     */
    public static <T> T newInstanceOf(final String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException,
                    NoSuchMethodException {
        final Class<T> clazz = Cast.cast(loadClass(className));
        return newInstanceOf(clazz);
    }

    /**
     * Loads and instantiates a class given by a property name.
     *
     * @param propertyName The property name to look up a class name for.
     * @param clazz        The class to cast it to.
     * @param <T>          The type to cast it to.
     * @return new instance of the class given in the property or {@code null} if the property was unset.
     * @throws ClassNotFoundException      if the class isn't available to the usual ClassLoaders
     * @throws ExceptionInInitializerError if an exception was thrown while initializing the class
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @throws ClassCastException          if the class is not compatible with the generic type parameter provided
     * @throws NoSuchMethodException       if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws IllegalAccessException      if the class can't be instantiated through a public constructor
     * @throws InstantiationException      if the provided class is abstract or an interface
     * @throws InvocationTargetException   if an exception is thrown by the constructor
     * @since 2.5
     */
    public static <T> T newCheckedInstanceOfProperty(final String propertyName, final Class<T> clazz)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
                    NoSuchMethodException {
        return newCheckedInstanceOfProperty(propertyName, clazz, () -> null);
    }

    /**
     * Loads and instantiates a class given by a property name.
     *
     * @param propertyName The property name to look up a class name for.
     * @param clazz        The class to cast it to.
     * @param defaultSupplier Supplier of a default value if the property is not present.
     * @param <T>          The type to cast it to.
     * @return new instance of the class given in the property or {@code null} if the property was unset.
     * @throws ClassNotFoundException      if the class isn't available to the usual ClassLoaders
     * @throws ExceptionInInitializerError if an exception was thrown while initializing the class
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @throws ClassCastException          if the class is not compatible with the generic type parameter provided
     * @throws NoSuchMethodException       if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws IllegalAccessException      if the class can't be instantiated through a public constructor
     * @throws InstantiationException      if the provided class is abstract or an interface
     * @throws InvocationTargetException   if an exception is thrown by the constructor
     * @since 2.22
     */
    public static <T> T newCheckedInstanceOfProperty(
            final String propertyName, final Class<T> clazz, final Supplier<T> defaultSupplier)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
                    NoSuchMethodException {
        final String className = PropertiesUtil.getProperties().getStringProperty(propertyName);
        if (className == null) {
            return defaultSupplier.get();
        }
        return newCheckedInstanceOf(className, clazz);
    }

    /**
     * Loads and instantiates a class by name using its default constructor. All checked reflective operation
     * exceptions are translated into corresponding {@link LinkageError} classes.
     *
     * @param className fully qualified class name to load, initialize, and construct
     * @param <T>       type the class must be compatible with
     * @return new instance of the class
     * @throws NoClassDefFoundError        if the specified class name could not be found
     * @throws ExceptionInInitializerError if an exception is thrown during class initialization
     * @throws ClassCastException          if the class is not compatible with the generic type parameter provided
     * @throws NoSuchMethodError           if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws InternalException           if an exception is thrown by the constructor
     * @throws InstantiationError          if the provided class is abstract or an interface
     * @throws IllegalAccessError          if the class cannot be accessed
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @since 2.22.0
     */
    public static <T> T newInstanceOfUnchecked(final String className) {
        final Class<T> clazz = Cast.cast(loadClassUnchecked(className));
        return newInstanceOfUnchecked(clazz);
    }

    /**
     * Loads and instantiates a derived class using its default constructor.
     *
     * @param className The class name.
     * @param clazz     The class to cast it to.
     * @param <T>       The type of the class to check.
     * @return new instance of the class cast to {@code T}
     * @throws ClassNotFoundException      if the class isn't available to the usual ClassLoaders
     * @throws ExceptionInInitializerError if an exception is thrown during class initialization
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @throws ClassCastException          if the constructed object isn't type compatible with {@code T}
     * @throws NoSuchMethodException       if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws IllegalAccessException      if the class can't be instantiated through a public constructor
     * @throws InstantiationException      if the provided class is abstract or an interface
     * @throws InvocationTargetException   if there was an exception whilst constructing the class
     * @since 2.1
     */
    public static <T> T newCheckedInstanceOf(final String className, final Class<T> clazz)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
                    NoSuchMethodException {
        return newInstanceOf(loadClass(className).asSubclass(clazz));
    }

    /**
     * Loads the provided class by name as a checked subtype of the given class. All checked reflective operation
     * exceptions are translated into corresponding {@link LinkageError} classes.
     *
     * @param className fully qualified class name to load
     * @param supertype supertype of the class being loaded
     * @param <T>       type of instance to return
     * @return new instance of the requested class
     * @throws NoClassDefFoundError        if the provided class name could not be found
     * @throws ExceptionInInitializerError if an exception is thrown during class initialization
     * @throws ClassCastException          if the loaded class is not a subtype of the provided class
     * @throws NoSuchMethodError           if no zero-arg constructor exists
     * @throws SecurityException           if this class is not allowed to access declared members of the provided class
     * @throws InternalException           if an exception is thrown by the constructor
     * @throws InstantiationError          if the provided class is abstract or an interface
     * @throws IllegalAccessError          if the class cannot be accessed
     * @throws LinkageError                if the linkage of the class fails for any other reason
     * @since 2.22.0
     */
    public static <T> T newInstanceOfUnchecked(final String className, final Class<T> supertype) {
        final Class<? extends T> clazz = loadClassUnchecked(className).asSubclass(supertype);
        return newInstanceOfUnchecked(clazz);
    }

    private static boolean isIgnoreTccl() {
        // we need to lazily initialize this, but concurrent access is not an issue
        if (ignoreTCCL == null) {
            final String ignoreTccl = PropertiesUtil.getProperties().getStringProperty(IGNORE_TCCL_PROPERTY, null);
            ignoreTCCL = ignoreTccl != null && !"false".equalsIgnoreCase(ignoreTccl.trim());
        }
        return ignoreTCCL;
    }

    /**
     * Finds classpath {@linkplain URL resources}.
     *
     * @param resource the name of the resource to find.
     * @return a Collection of URLs matching the resource name. If no resources could be found, then this will be empty.
     * @since 2.1
     */
    public static Collection<URL> findResources(final String resource) {
        return findResources(resource, true);
    }

    static Collection<URL> findResources(final String resource, final boolean useTccl) {
        final Collection<UrlResource> urlResources = findUrlResources(resource, useTccl);
        final Collection<URL> resources = new LinkedHashSet<>(urlResources.size());
        for (final UrlResource urlResource : urlResources) {
            resources.add(urlResource.getUrl());
        }
        return resources;
    }

    static Collection<UrlResource> findUrlResources(final String resource, final boolean useTccl) {
        // @formatter:off
        final ClassLoader[] candidates = {
            useTccl ? getThreadContextClassLoader() : null,
            LoaderUtil.class.getClassLoader(),
            GET_CLASS_LOADER_DISABLED.getAsBoolean() ? null : ClassLoader.getSystemClassLoader()
        };
        // @formatter:on
        final Collection<UrlResource> resources = new LinkedHashSet<>();
        for (final ClassLoader cl : candidates) {
            if (cl != null) {
                try {
                    final Enumeration<URL> resourceEnum = cl.getResources(resource);
                    while (resourceEnum.hasMoreElements()) {
                        resources.add(new UrlResource(cl, resourceEnum.nextElement()));
                    }
                } catch (final IOException error) {
                    LOGGER.error("failed to collect resources of name `{}`", resource, error);
                }
            }
        }
        return resources;
    }

    /**
     * {@link URL} and {@link ClassLoader} pair.
     */
    static class UrlResource {
        private final ClassLoader classLoader;
        private final URL url;

        UrlResource(final ClassLoader classLoader, final URL url) {
            this.classLoader = classLoader;
            this.url = url;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public URL getUrl() {
            return url;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UrlResource)) {
                return false;
            }

            final UrlResource that = (UrlResource) o;

            return Objects.equals(classLoader, that.classLoader) && Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(classLoader) + Objects.hashCode(url);
        }
    }
}

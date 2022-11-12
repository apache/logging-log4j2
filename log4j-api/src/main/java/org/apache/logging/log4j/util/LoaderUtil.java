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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.apache.logging.log4j.spi.LoggingSystemProperties;

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

    private static final ClassLoader[] EMPTY_CLASS_LOADER_ARRAY = {};

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    // this variable must be lazily loaded; otherwise, we get a nice circular class loading problem where LoaderUtil
    // wants to use PropertiesUtil, but then PropertiesUtil wants to use LoaderUtil.
    private static Boolean ignoreTCCL;

    private static final boolean GET_CLASS_LOADER_DISABLED;

    protected static Boolean forceTcclOnly;

    private static final PrivilegedAction<ClassLoader> TCCL_GETTER = new ThreadContextClassLoaderGetter();

    static {
        if (SECURITY_MANAGER != null) {
            boolean getClassLoaderDisabled;
            try {
                SECURITY_MANAGER.checkPermission(new RuntimePermission("getClassLoader"));
                getClassLoaderDisabled = false;
            } catch (final SecurityException ignored) {
                getClassLoaderDisabled = true;
            }
            GET_CLASS_LOADER_DISABLED = getClassLoaderDisabled;
        } else {
            GET_CLASS_LOADER_DISABLED = false;
        }
    }

    private LoaderUtil() {
    }

    /**
     * Returns the ClassLoader to use.
     * @return the ClassLoader.
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(LoaderUtil.class, null);
    }

    // TODO: this method could use some explanation
    public static ClassLoader getClassLoader(final Class<?> class1, final Class<?> class2) {
        final ClassLoader threadContextClassLoader = getThreadContextClassLoader();
        final ClassLoader loader1 = class1 == null ? null : class1.getClassLoader();
        final ClassLoader loader2 = class2 == null ? null : class2.getClassLoader();

        if (isChild(threadContextClassLoader, loader1)) {
            return isChild(threadContextClassLoader, loader2) ? threadContextClassLoader : loader2;
        }
        return isChild(loader1, loader2) ? loader1 : loader2;
    }

    /**
     * Gets the current Thread ClassLoader. Returns the system ClassLoader if the TCCL is {@code null}. If the system
     * ClassLoader is {@code null} as well, then the ClassLoader for this class is returned. If running with a
     * {@link SecurityManager} that does not allow access to the Thread ClassLoader or system ClassLoader, then the
     * ClassLoader for this class is returned.
     *
     * @return the current ThreadContextClassLoader.
     */
    public static ClassLoader getThreadContextClassLoader() {
        if (GET_CLASS_LOADER_DISABLED) {
            // we can at least get this class's ClassLoader regardless of security context
            // however, if this is null, there's really no option left at this point
            return LoaderUtil.class.getClassLoader();
        }
        return SECURITY_MANAGER == null ? TCCL_GETTER.run() : AccessController.doPrivileged(TCCL_GETTER);
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
     *
     */
    private static class ThreadContextClassLoaderGetter implements PrivilegedAction<ClassLoader> {
        @Override
        public ClassLoader run() {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                return cl;
            }
            final ClassLoader ccl = LoaderUtil.class.getClassLoader();
            return ccl == null && !GET_CLASS_LOADER_DISABLED ? ClassLoader.getSystemClassLoader() : ccl;
        }
    }

    public static ClassLoader[] getClassLoaders() {
        final Collection<ClassLoader> classLoaders = new LinkedHashSet<>();
        final ClassLoader tcl = getThreadContextClassLoader();
        if (tcl != null) {
            classLoaders.add(tcl);
        }
        final ModuleLayer layer = LoaderUtil.class.getModule().getLayer();
        if (layer == null) {
            if (!isForceTccl()) {
                accumulateClassLoaders(LoaderUtil.class.getClassLoader(), classLoaders);
                accumulateClassLoaders(tcl == null ? null : tcl.getParent(), classLoaders);
                final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                if (systemClassLoader != null) {
                    classLoaders.add(systemClassLoader);
                }
            }
        } else {
            accumulateLayerClassLoaders(layer, classLoaders);
            if (layer != ModuleLayer.boot()) {
                for (final Module module : ModuleLayer.boot().modules()) {
                    accumulateClassLoaders(module.getClassLoader(), classLoaders);
                }
            }
        }
        return classLoaders.toArray(EMPTY_CLASS_LOADER_ARRAY);
    }

    private static void accumulateLayerClassLoaders(final ModuleLayer layer, final Collection<ClassLoader> classLoaders) {
        for (final Module module : layer.modules()) {
            accumulateClassLoaders(module.getClassLoader(), classLoaders);
        }
        if (layer.parents().size() > 0) {
            for (final ModuleLayer parent : layer.parents()) {
                accumulateLayerClassLoaders(parent, classLoaders);
            }
        }
    }

    /**
     * Adds the provided loader to the loaders collection, and traverses up the tree until either a null
     * value or a classloader which has already been added is encountered.
     */
    private static void accumulateClassLoaders(final ClassLoader loader, final Collection<ClassLoader> loaders) {
        // Some implementations may use null to represent the bootstrap class loader.
        if (loader != null && loaders.add(loader)) {
            accumulateClassLoaders(loader.getParent(), loaders);
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
            final Class<?> clazz = loadClass(className);
            return clazz != null;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        } catch (final Throwable e) {
            LowLevelLogUtil.logException("Unknown error checking for existence of class: " + className, e);
            return false;
        }
    }

    /**
     * Loads a class by name. This method respects the {@value LoggingSystemProperties#LOADER_IGNORE_THREAD_CONTEXT_LOADER}
     * Log4j property. If this property is specified and set to anything besides {@code false}, then this class's
     * ClassLoader will be used as specified by {@link Class#forName(String)}.
     *
     * @param className The class name.
     * @return the Class for the given name.
     * @throws ClassNotFoundException if the specified class name could not be found
     * @since 2.1
     */
    public static Class<?> loadClass(final String className) throws ClassNotFoundException {
        if (isIgnoreTccl()) {
            return Class.forName(className);
        }
        try {
            final ClassLoader tccl = getThreadContextClassLoader();
            if (tccl != null) {
                return tccl.loadClass(className);
            }
        } catch (final Throwable ignored) {
        }
        return Class.forName(className);
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param <T> the type of the class modeled by the {@code Class} object.
     * @param clazz The class.
     * @return new instance of the class.
     * @throws IllegalAccessException if the class can't be instantiated through a public constructor
     * @throws InstantiationException if there was an exception whilst instantiating the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @since 2.7
     */
    public static <T> T newInstanceOf(final Class<T> clazz)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            return clazz.getConstructor().newInstance();
        } catch (final NoSuchMethodException ignored) {
            // FIXME: looking at the code for Class.newInstance(), this seems to do the same thing as above
            return clazz.newInstance();
        }
    }

    /**
     * Loads and instantiates a Class using the default constructor.
     *
     * @param className The class name.
     * @param <T> The class's type.
     * @return new instance of the class.
     * @throws ClassNotFoundException if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException if the class can't be instantiated through a public constructor
     * @throws InstantiationException if there was an exception whilst instantiating the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstanceOf(final String className) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        return newInstanceOf((Class<T>) loadClass(className));
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
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @throws ClassCastException if the constructed object isn't type compatible with {@code T}
     * @since 2.1
     */
    public static <T> T newCheckedInstanceOf(final String className, final Class<T> clazz)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        return newInstanceOf(loadClass(className).asSubclass(clazz));
    }

    /**
     * Loads and instantiates a class given by a property name.
     *
     * @param propertyName The property name to look up a class name for.
     * @param clazz        The class to cast it to.
     * @param <T>          The type to cast it to.
     * @return new instance of the class given in the property or {@code null} if the property was unset.
     * @throws ClassNotFoundException    if the class isn't available to the usual ClassLoaders
     * @throws IllegalAccessException    if the class can't be instantiated through a public constructor
     * @throws InstantiationException    if there was an exception whilst instantiating the class
     * @throws InvocationTargetException if there was an exception whilst constructing the class
     * @throws ClassCastException        if the constructed object isn't type compatible with {@code T}
     * @since 2.5
     */
    public static <T> T newCheckedInstanceOfProperty(final String propertyName, final Class<T> clazz)
        throws ClassNotFoundException, InvocationTargetException, InstantiationException,
        IllegalAccessException {
        final String className = PropertiesUtil.getProperties().getStringProperty(propertyName);
        if (className == null) {
            return null;
        }
        return newCheckedInstanceOf(className, clazz);
    }

    private static boolean isIgnoreTccl() {
        // we need to lazily initialize this, but concurrent access is not an issue
        if (ignoreTCCL == null) {
            final String ignoreTccl = PropertiesUtil.getProperties().getStringProperty(LoggingSystemProperties.LOADER_IGNORE_THREAD_CONTEXT_LOADER, null);
            ignoreTCCL = ignoreTccl != null && !"false".equalsIgnoreCase(ignoreTccl.trim());
        }
        return ignoreTCCL;
    }

    private static boolean isForceTccl() {
        if (forceTcclOnly == null) {
            // PropertiesUtil.getProperties() uses that code path so don't use that!
            try {
                forceTcclOnly = System.getSecurityManager() == null ?
                    Boolean.getBoolean(LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER) :
                    AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean(LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER));
            } catch (final SecurityException se) {
                forceTcclOnly = false;
            }
        }
        return forceTcclOnly;
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

    public static Collection<URL> findResources(final String resource, final boolean useTccl) {
        final Collection<UrlResource> urlResources = findUrlResources(resource, useTccl);
        final Collection<URL> resources = new LinkedHashSet<>(urlResources.size());
        for (final UrlResource urlResource : urlResources) {
            resources.add(urlResource.getUrl());
        }
        return resources;
    }

    /**
     * This method will only find resources that follow the JPMS rules for encapsulation. Resources
     * on the class path should be found as normal along with resources with no package name in all
     * modules. Resources within packages in modules must declare those resources open to org.apache.logging.log4j.
     * @param resource The resource to locate.
     * @return The located resources.
     */
    public static Collection<UrlResource> findUrlResources(final String resource, boolean useTccl) {
        // @formatter:off
        final ClassLoader[] candidates = {
                getThreadContextClassLoader(),
                isForceTccl() ? null : LoaderUtil.class.getClassLoader(),
                isForceTccl() || GET_CLASS_LOADER_DISABLED ? null : ClassLoader.getSystemClassLoader()};
        // @formatter:on
        final Collection<UrlResource> resources = new LinkedHashSet<>();
        for (final ClassLoader cl : candidates) {
            if (cl != null) {
                try {
                    final Enumeration<URL> resourceEnum = cl.getResources(resource);
                    while (resourceEnum.hasMoreElements()) {
                        resources.add(new UrlResource(cl, resourceEnum.nextElement()));
                    }
                } catch (final IOException e) {
                    LowLevelLogUtil.logException(e);
                }
            }
        }
        return resources;
    }

    /**
     * {@link URL} and {@link ClassLoader} pair.
     */
    public static class UrlResource {
        private final ClassLoader classLoader;
        private final URL url;

        public UrlResource(final ClassLoader classLoader, final URL url) {
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
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final UrlResource that = (UrlResource) o;

            if (classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null) {
                return false;
            }
            return url != null ? url.equals(that.url) : that.url == null;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(classLoader) + Objects.hashCode(url);
        }
    }
}

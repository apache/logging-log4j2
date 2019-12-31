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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * {@link ThrowableProxyHelper} provides utilities required to initialize a new {@link ThrowableProxy}
 * instance.
 */
class ThrowableProxyHelper {

    private static final ExtendedClassInfo UNKNOWN_EXTENDED_CLASS_INFO = new ExtendedClassInfo(false, "?", "?");
    private static final CacheEntry UNKNOWN_CACHE_ENTRY = new CacheEntry(UNKNOWN_EXTENDED_CLASS_INFO, null);
    static final ThrowableProxy[] EMPTY_THROWABLE_PROXY_ARRAY = new ThrowableProxy[0];

    private ThrowableProxyHelper() {
        // Utility Class
    }

    /**
     * Cached StackTracePackageElement and ClassLoader.
     * <p>
     * Consider this class private.
     * </p>
     */
    static final class CacheEntry {
        private final ExtendedClassInfo element;
        private final ClassLoader loader;

        private CacheEntry(final ExtendedClassInfo element, final ClassLoader loader) {
            this.element = element;
            this.loader = loader;
        }
    }

    /**
     * Resolve all the stack entries in this stack trace that are not common with the parent.
     *
     * @param src        Instance for which to build an extended stack trace.
     * @param stack      The callers Class stack.
     * @param map        The cache of CacheEntry objects.
     * @param rootTrace  The first stack trace resolve or null.
     * @param stackTrace The stack trace being resolved.
     * @return The StackTracePackageElement array.
     */
    static ExtendedStackTraceElement[] toExtendedStackTrace(
            final ThrowableProxy src,
            final Stack<Class<?>> stack,
            final Map<String, CacheEntry> map,
            final StackTraceElement[] rootTrace,
            final StackTraceElement[] stackTrace) {
        int stackLength;
        if (rootTrace != null) {
            int rootIndex = rootTrace.length - 1;
            int stackIndex = stackTrace.length - 1;
            while (rootIndex >= 0 && stackIndex >= 0 && rootTrace[rootIndex].equals(stackTrace[stackIndex])) {
                --rootIndex;
                --stackIndex;
            }
            src.setCommonElementCount(stackTrace.length - 1 - stackIndex);
            stackLength = stackIndex + 1;
        } else {
            src.setCommonElementCount(0);
            stackLength = stackTrace.length;
        }
        final ExtendedStackTraceElement[] extStackTrace = new ExtendedStackTraceElement[stackLength];
        ClassLoader lastLoader = null;
        for (int i = stackLength - 1; i >= 0; --i) {
            final StackTraceElement stackTraceElement = stackTrace[i];
            final String className = stackTraceElement.getClassName();
            // The stack returned from getCurrentStack may be missing entries for java.lang.reflect.Method.invoke()
            // and its implementation. The Throwable might also contain stack entries that are no longer
            // present as those methods have returned.
            ExtendedClassInfo extClassInfo;
            boolean avoidClassLoading = avoidClassLoading(className);
            final CacheEntry cacheEntry = avoidClassLoading ? null : map.get(className);
            if (cacheEntry != null) {
                extClassInfo = cacheEntry.element;
                if (cacheEntry.loader != null) {
                    lastLoader = cacheEntry.loader;
                }
            } else if (avoidClassLoading) {
                extClassInfo = UNKNOWN_EXTENDED_CLASS_INFO;
            } else {
                final CacheEntry entry = toCacheEntry(loadClass(lastLoader, className), false);
                extClassInfo = entry.element;
                map.put(className, entry);
                if (entry.loader != null) {
                    lastLoader = entry.loader;
                }
            }
            extStackTrace[i] = new ExtendedStackTraceElement(stackTraceElement, extClassInfo);
        }
        return extStackTrace;
    }

    /**
     * Returns whether or not class loading should be attempted for a given name.
     *
     * Proxies do not provide location information, though they can be loaded it isn't relevant for
     * ThrowableProxy.
     * Lambda classes cannot be loaded, nor can generated method accessors.
     * Class loading is expensive, but failure to load classes is significantly more expensive.
     */
    private static boolean avoidClassLoading(String className) {
        // Lambdas and proxies do not provide location information
        // attempt to avoid anything that does not provide information, class loader
        // misses are expensive and incur synchronization.
        return className.contains("$Lambda$")
                || className.contains(".$Proxy")
                // covers both jdk.internal and sun class names depending on the JVM version
                || className.contains("reflect.GeneratedMethodAccessor");
    }

    static ThrowableProxy[] toSuppressedProxies(final Throwable thrown, Set<Throwable> suppressedVisited) {
        try {
            final Throwable[] suppressed = thrown.getSuppressed();
            if (suppressed == null || suppressed.length == 0) {
                return EMPTY_THROWABLE_PROXY_ARRAY;
            }
            final List<ThrowableProxy> proxies = new ArrayList<>(suppressed.length);
            if (suppressedVisited == null) {
                suppressedVisited = new HashSet<>(suppressed.length);
            }
            for (int i = 0; i < suppressed.length; i++) {
                final Throwable candidate = suppressed[i];
                if (suppressedVisited.add(candidate)) {
                    proxies.add(new ThrowableProxy(candidate, suppressedVisited));
                }
            }
            return proxies.toArray(new ThrowableProxy[proxies.size()]);
        } catch (final Exception e) {
            StatusLogger.getLogger().error(e);
        }
        return null;
    }

    static Map<String, CacheEntry> createCacheFromStack(Stack<Class<?>> callerStack) {
        Map<String, CacheEntry> cache = new HashMap<>();
        for (Class<?> callerClass : callerStack) {
            String className = callerClass.getName();
            if (!avoidClassLoading(className) && !cache.containsKey(className)) {
                cache.put(className, toCacheEntry(callerClass, true));
            }
        }
        return cache;
    }

    /**
     * Construct the CacheEntry from the Class's information.
     *
     * @param callerClass       The Class.
     * @param exact             True if the class was obtained via Reflection.getCallerClass.
     * @return The CacheEntry.
     */
    private static CacheEntry toCacheEntry(final Class<?> callerClass, final boolean exact) {
        String location = "?";
        String version = "?";
        ClassLoader lastLoader = null;
        if (callerClass != null) {
            try {
                final CodeSource source = callerClass.getProtectionDomain().getCodeSource();
                if (source != null) {
                    final URL locationURL = source.getLocation();
                    if (locationURL != null) {
                        final String str = locationURL.toString().replace('\\', '/');
                        int index = str.lastIndexOf("/");
                        if (index >= 0 && index == str.length() - 1) {
                            index = str.lastIndexOf("/", index - 1);
                            location = str.substring(index + 1);
                        } else {
                            location = str.substring(index + 1);
                        }
                    }
                }
            } catch (final Exception ex) {
                // Ignore the exception.
            }
            final Package pkg = callerClass.getPackage();
            if (pkg != null) {
                final String ver = pkg.getImplementationVersion();
                if (ver != null) {
                    version = ver;
                }
            }
            try {
                lastLoader = callerClass.getClassLoader();
            } catch (final SecurityException e) {
                lastLoader = null;
            }
            return new CacheEntry(new ExtendedClassInfo(exact, location, version), lastLoader);
        }
        return UNKNOWN_CACHE_ENTRY;
    }


    /**
     * Loads classes not located via Reflection.getCallerClass.
     *
     * @param lastLoader The ClassLoader that loaded the Class that called this Class.
     * @param className  The name of the Class.
     * @return The Class object for the Class or null if it could not be located.
     */
    private static Class<?> loadClass(final ClassLoader lastLoader, final String className) {
        // XXX: this is overly complicated
        Class<?> clazz;
        if (lastLoader != null) {
            try {
                clazz = lastLoader.loadClass(className);
                if (clazz != null) {
                    return clazz;
                }
            } catch (final Throwable ignore) {
                // Ignore exception.
            }
        }
        try {
            clazz = LoaderUtil.loadClass(className);
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            return loadClass(className);
        } catch (final SecurityException e) {
            return null;
        }
        return clazz;
    }

    private static Class<?> loadClass(final String className) {
        try {
            return Loader.loadClass(className, ThrowableProxyHelper.class.getClassLoader());
        } catch (final ClassNotFoundException | NoClassDefFoundError | SecurityException e) {
            return null;
        }
    }
}

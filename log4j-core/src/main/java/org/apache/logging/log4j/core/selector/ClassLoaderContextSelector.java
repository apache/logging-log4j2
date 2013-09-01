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
package org.apache.logging.log4j.core.selector;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.impl.ReflectiveCallerClassUtility;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This ContextSelector chooses a LoggerContext based upon the ClassLoader of the caller. This allows Loggers
 * assigned to static variables to be released along with the classes that own then. Other ContextSelectors
 * will generally cause Loggers associated with classes loaded from different ClassLoaders to be co-mingled.
 * This is a problem if, for example, a web application is undeployed as some of the Loggers being released may be
 * associated with a Class in a parent ClassLoader, which will generally have negative consequences.
 *
 * The main downside to this ContextSelector is that Configuration is more challenging.
 *
 * This ContextSelector should not be used with a Servlet Filter such as the Log4jServletFilter.
 */
public class ClassLoaderContextSelector implements ContextSelector {

    private static final AtomicReference<LoggerContext> CONTEXT = new AtomicReference<LoggerContext>();

    private static final PrivateSecurityManager SECURITY_MANAGER;

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final ConcurrentMap<String, AtomicReference<WeakReference<LoggerContext>>> CONTEXT_MAP =
        new ConcurrentHashMap<String, AtomicReference<WeakReference<LoggerContext>>>();

    static {
        if (ReflectiveCallerClassUtility.isSupported()) {
            SECURITY_MANAGER = null;
        } else {
            PrivateSecurityManager securityManager;
            try {
                securityManager = new PrivateSecurityManager();
                if (securityManager.getCaller(ClassLoaderContextSelector.class.getName()) == null) {
                    // This shouldn't happen.
                    securityManager = null;
                    LOGGER.error("Unable to obtain call stack from security manager.");
                }
            } catch (final Exception e) {
                securityManager = null;
                LOGGER.debug("Unable to install security manager", e);
            }
            SECURITY_MANAGER = securityManager;
        }
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        return getContext(fqcn, loader, currentContext, null);
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
                                    final URI configLocation) {
        if (currentContext) {
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        } else if (loader != null) {
            return locateContext(loader, configLocation);
        } else {
            if (ReflectiveCallerClassUtility.isSupported()) {
                try {
                    Class<?> clazz = Class.class;
                    boolean next = false;
                    for (int index = 2; clazz != null; ++index) {
                        clazz = ReflectiveCallerClassUtility.getCaller(index);
                        if (clazz == null) {
                            break;
                        }
                        if (clazz.getName().equals(fqcn)) {
                            next = true;
                            continue;
                        }
                        if (next) {
                            break;
                        }
                    }
                    if (clazz != null) {
                        return locateContext(clazz.getClassLoader(), configLocation);
                    }
                } catch (final Exception ex) {
                    // logger.debug("Unable to determine caller class via Sun Reflection", ex);
                }
            }

            if (SECURITY_MANAGER != null) {
                final Class<?> clazz = SECURITY_MANAGER.getCaller(fqcn);
                if (clazz != null) {
                    final ClassLoader ldr = clazz.getClassLoader() != null ? clazz.getClassLoader() :
                        ClassLoader.getSystemClassLoader();
                    return locateContext(ldr, configLocation);
                }
            }

            final Throwable t = new Throwable();
            boolean next = false;
            String name = null;
            for (final StackTraceElement element : t.getStackTrace()) {
                if (element.getClassName().equals(fqcn)) {
                    next = true;
                    continue;
                }
                if (next) {
                    name = element.getClassName();
                    break;
                }
            }
            if (name != null) {
                try {
                    return locateContext(Loader.loadClass(name).getClassLoader(), configLocation);
                } catch (final ClassNotFoundException ignore) {
                    //this is ok
                }
            }
            final LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
            if (lc != null) {
                return lc;
            }
            return getDefault();
        }
    }

    @Override
    public void removeContext(final LoggerContext context) {
        for (final Map.Entry<String, AtomicReference<WeakReference<LoggerContext>>> entry : CONTEXT_MAP.entrySet()) {
            final LoggerContext ctx = entry.getValue().get().get();
            if (ctx == context) {
                CONTEXT_MAP.remove(entry.getKey());
            }
        }
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        final List<LoggerContext> list = new ArrayList<LoggerContext>();
        final Collection<AtomicReference<WeakReference<LoggerContext>>> coll = CONTEXT_MAP.values();
        for (final AtomicReference<WeakReference<LoggerContext>> ref : coll) {
            final LoggerContext ctx = ref.get().get();
            if (ctx != null) {
                list.add(ctx);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private LoggerContext locateContext(final ClassLoader loader, final URI configLocation) {
        final String name = loader.toString();
        AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref == null) {
            if (configLocation == null) {
                ClassLoader parent = loader.getParent();
                while (parent != null) {

                    ref = CONTEXT_MAP.get(parent.toString());
                    if (ref != null) {
                        final WeakReference<LoggerContext> r = ref.get();
                        final LoggerContext ctx = r.get();
                        if (ctx != null) {
                            return ctx;
                        }
                    }
                    parent = parent.getParent();
                    /*  In Tomcat 6 the parent of the JSP classloader is the webapp classloader which would be
                    configured by the WebAppContextListener. The WebAppClassLoader is also the ThreadContextClassLoader.
                    In JBoss 5 the parent of the JSP ClassLoader is the WebAppClassLoader which is also the
                    ThreadContextClassLoader. However, the parent of the WebAppClassLoader is the ClassLoader
                    that is configured by the WebAppContextListener.

                    ClassLoader threadLoader = null;
                    try {
                        threadLoader = Thread.currentThread().getContextClassLoader();
                    } catch (Exception ex) {
                        // Ignore SecurityException
                    }
                    if (threadLoader != null && threadLoader == parent) {
                        break;
                    } else {
                        parent = parent.getParent();
                    } */
                }
            }
            LoggerContext ctx = new LoggerContext(name, null, configLocation);
            final AtomicReference<WeakReference<LoggerContext>> r =
                new AtomicReference<WeakReference<LoggerContext>>();
            r.set(new WeakReference<LoggerContext>(ctx));
            CONTEXT_MAP.putIfAbsent(loader.toString(), r);
            ctx = CONTEXT_MAP.get(name).get().get();
            return ctx;
        }
        final WeakReference<LoggerContext> r = ref.get();
        LoggerContext ctx = r.get();
        if (ctx != null) {
            return ctx;
        }
        ctx = new LoggerContext(name, null, configLocation);
        ref.compareAndSet(r, new WeakReference<LoggerContext>(ctx));
        return ctx;
    }

    private LoggerContext getDefault() {
        final LoggerContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx;
        }
        CONTEXT.compareAndSet(null, new LoggerContext("Default"));
        return CONTEXT.get();
    }

    /**
     * SecurityManager that will locate the caller of the Log4j 2 API.
     */
    private static class PrivateSecurityManager extends SecurityManager {

        public Class<?> getCaller(final String fqcn) {
            final Class<?>[] classes = getClassContext();
            boolean next = false;
            for (final Class<?> clazz : classes) {
                if (clazz.getName().equals(fqcn)) {
                    next = true;
                    continue;
                }
                if (next) {
                    return clazz;
                }
            }
            return null;
        }
    }

}

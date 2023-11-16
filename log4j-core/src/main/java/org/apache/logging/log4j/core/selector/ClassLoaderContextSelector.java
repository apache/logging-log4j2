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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.spi.LoggerContextShutdownAware;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * This ContextSelector chooses a LoggerContext based upon the ClassLoader of the caller. This allows Loggers assigned
 * to static variables to be released along with the classes that own then. Other ContextSelectors will generally cause
 * Loggers associated with classes loaded from different ClassLoaders to be co-mingled. This is a problem if, for
 * example, a web application is undeployed as some of the Loggers being released may be associated with a Class in a
 * parent ClassLoader, which will generally have negative consequences.
 *
 * The main downside to this ContextSelector is that Configuration is more challenging.
 *
 * This ContextSelector should not be used with a Servlet Filter such as the Log4jServletFilter.
 */
public class ClassLoaderContextSelector implements ContextSelector, LoggerContextShutdownAware {

    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    protected static final ConcurrentMap<String, AtomicReference<WeakReference<LoggerContext>>> CONTEXT_MAP =
            new ConcurrentHashMap<>();

    private final Lazy<LoggerContext> defaultLoggerContext = Lazy.lazy(() -> createContext(defaultContextName(), null));

    @Override
    public void shutdown(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
        LoggerContext ctx = null;
        if (currentContext) {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
        } else if (loader != null) {
            ctx = findContext(loader);
        } else {
            final Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
            if (clazz != null) {
                ctx = findContext(clazz.getClassLoader());
            }
            if (ctx == null) {
                ctx = ContextAnchor.THREAD_CONTEXT.get();
            }
        }
        if (ctx != null) {
            ctx.stop(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void contextShutdown(final org.apache.logging.log4j.spi.LoggerContext loggerContext) {
        if (loggerContext instanceof LoggerContext) {
            removeContext((LoggerContext) loggerContext);
        }
    }

    @Override
    public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        LoggerContext ctx;
        if (currentContext) {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
        } else if (loader != null) {
            ctx = findContext(loader);
        } else {
            final Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
            if (clazz != null) {
                ctx = findContext(clazz.getClassLoader());
            } else {
                ctx = ContextAnchor.THREAD_CONTEXT.get();
            }
        }
        return ctx != null && ctx.isStarted();
    }

    private LoggerContext findContext(final ClassLoader loaderOrNull) {
        final ClassLoader loader = loaderOrNull != null ? loaderOrNull : ClassLoader.getSystemClassLoader();
        final String name = toContextMapKey(loader);
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref != null) {
            final WeakReference<LoggerContext> weakRef = ref.get();
            return weakRef.get();
        }
        return null;
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        return getContext(fqcn, loader, currentContext, null);
    }

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
        return getContext(fqcn, loader, null, currentContext, configLocation);
    }

    @Override
    public LoggerContext getContext(
            final String fqcn,
            final ClassLoader loader,
            final Map.Entry<String, Object> entry,
            final boolean currentContext,
            final URI configLocation) {
        if (currentContext) {
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        } else if (loader != null) {
            return locateContext(loader, entry, configLocation);
        } else {
            final Class<?> clazz = StackLocatorUtil.getCallerClass(fqcn);
            if (clazz != null) {
                return locateContext(clazz.getClassLoader(), entry, configLocation);
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
    public boolean isClassLoaderDependent() {
        // By definition the ClassLoaderContextSelector depends on the callers class loader.
        return true;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        final List<LoggerContext> list = new ArrayList<>();
        final Collection<AtomicReference<WeakReference<LoggerContext>>> coll = CONTEXT_MAP.values();
        for (final AtomicReference<WeakReference<LoggerContext>> ref : coll) {
            final LoggerContext ctx = ref.get().get();
            if (ctx != null) {
                list.add(ctx);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private LoggerContext locateContext(
            final ClassLoader loaderOrNull, final Map.Entry<String, Object> entry, final URI configLocation) {
        // LOG4J2-477: class loader may be null
        final ClassLoader loader = loaderOrNull != null ? loaderOrNull : ClassLoader.getSystemClassLoader();
        final String name = toContextMapKey(loader);
        AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref == null) {
            if (configLocation == null) {
                ClassLoader parent = loader.getParent();
                while (parent != null) {

                    ref = CONTEXT_MAP.get(toContextMapKey(parent));
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
            final LoggerContext ctx = createContext(name, configLocation);
            if (entry != null) {
                ctx.putObject(entry.getKey(), entry.getValue());
            }
            final LoggerContext newContext = CONTEXT_MAP
                    .computeIfAbsent(name, k -> new AtomicReference<>(new WeakReference<>(ctx)))
                    .get()
                    .get();
            if (newContext == ctx) {
                ctx.addShutdownListener(this);
            }
            return newContext;
        }
        final WeakReference<LoggerContext> weakRef = ref.get();
        LoggerContext ctx = weakRef.get();
        if (ctx != null) {
            if (entry != null && ctx.getObject(entry.getKey()) == null) {
                ctx.putObject(entry.getKey(), entry.getValue());
            }
            if (ctx.getConfigLocation() == null && configLocation != null) {
                LOGGER.debug("Setting configuration to {}", configLocation);
                ctx.setConfigLocation(configLocation);
            } else if (ctx.getConfigLocation() != null
                    && configLocation != null
                    && !ctx.getConfigLocation().equals(configLocation)) {
                LOGGER.warn(
                        "locateContext called with URI {}. Existing LoggerContext has URI {}",
                        configLocation,
                        ctx.getConfigLocation());
            }
            return ctx;
        }
        ctx = createContext(name, configLocation);
        if (entry != null) {
            ctx.putObject(entry.getKey(), entry.getValue());
        }
        ref.compareAndSet(weakRef, new WeakReference<>(ctx));
        return ctx;
    }

    protected LoggerContext createContext(final String name, final URI configLocation) {
        return new LoggerContext(name, null, configLocation);
    }

    protected String toContextMapKey(final ClassLoader loader) {
        return Integer.toHexString(System.identityHashCode(loader));
    }

    protected LoggerContext getDefault() {
        return defaultLoggerContext.get();
    }

    protected String defaultContextName() {
        return "Default";
    }
}

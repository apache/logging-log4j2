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
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReflectionUtil;

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

    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    protected static final ConcurrentMap<String, AtomicReference<WeakReference<LoggerContext>>> CONTEXT_MAP =
        new ConcurrentHashMap<String, AtomicReference<WeakReference<LoggerContext>>>();

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
            final Class<?> clazz = ReflectionUtil.getCallerClass(fqcn);
            if (clazz != null) {
                return locateContext(clazz.getClassLoader(), configLocation);
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

    private LoggerContext locateContext(final ClassLoader loaderOrNull, final URI configLocation) {
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
            LoggerContext ctx = new LoggerContext(name, null, configLocation);
            final AtomicReference<WeakReference<LoggerContext>> r =
                new AtomicReference<WeakReference<LoggerContext>>();
            r.set(new WeakReference<LoggerContext>(ctx));
            CONTEXT_MAP.putIfAbsent(name, r);
            ctx = CONTEXT_MAP.get(name).get().get();
            return ctx;
        }
        final WeakReference<LoggerContext> weakRef = ref.get();
        LoggerContext ctx = weakRef.get();
        if (ctx != null) {
            if (ctx.getConfigLocation() == null && configLocation != null) {
                LOGGER.debug("Setting configuration to {}", configLocation);
                ctx.setConfigLocation(configLocation);
            } else if (ctx.getConfigLocation() != null && configLocation != null &&
                !ctx.getConfigLocation().equals(configLocation)) {
                LOGGER.warn("locateContext called with URI {}. Existing LoggerContext has URI {}", configLocation,
                    ctx.getConfigLocation());
            }
            return ctx;
        }
        ctx = new LoggerContext(name, null, configLocation);
        ref.compareAndSet(weakRef, new WeakReference<LoggerContext>(ctx));
        return ctx;
    }

    private String toContextMapKey(final ClassLoader loader) {
        return String.valueOf(System.identityHashCode(loader));
    }

    protected LoggerContext getDefault() {
        final LoggerContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx;
        }
        CONTEXT.compareAndSet(null, new LoggerContext("Default"));
        return CONTEXT.get();
    }

}

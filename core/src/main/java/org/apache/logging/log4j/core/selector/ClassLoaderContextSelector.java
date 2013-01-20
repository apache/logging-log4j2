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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This ContextSelector chooses a LoggerContext based upon the ClassLoader of the caller. This allows Loggers
 * assigned to static variables to be released along with the classes that own then. Other ContextSelectors
 * will generally cause Loggers associated with classes loaded from different ClassLoaders to be co-mingled.
 * This is a problem if, for example, a web application is undeployed as some of the Loggers being released may be
 * associated with a Class in a parent ClassLoader, which will generally have negative consequences.
 *
 * The main downside to this ContextSelector is that Configuration is more challenging.
 *
 * This ContextSelector should not be used with a Servlet Filter such as the JNDIContextFilter.
 */
public class ClassLoaderContextSelector implements ContextSelector {

    private static final AtomicReference<LoggerContext> CONTEXT = new AtomicReference<LoggerContext>();

    private static PrivateSecurityManager securityManager;

    private static Method getCallerClass;

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final ConcurrentMap<String, AtomicReference<WeakReference<LoggerContext>>> CONTEXT_MAP =
        new ConcurrentHashMap<String, AtomicReference<WeakReference<LoggerContext>>>();

    static {
        setupCallerCheck();
    }

    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        if (currentContext) {
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        } else if (loader != null) {
            return locateContext(loader, null);
        } else {
            if (getCallerClass != null) {
                try {
                    Class clazz = Class.class;
                    boolean next = false;
                    for (int index = 2; clazz != null; ++index) {
                        final Object[] params = new Object[] {index};
                        clazz = (Class) getCallerClass.invoke(null, params);
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
                        return locateContext(clazz.getClassLoader(), null);
                    }
                } catch (final Exception ex) {
                    // logger.debug("Unable to determine caller class via Sun Reflection", ex);
                }
            }

            if (securityManager != null) {
                final Class clazz = securityManager.getCaller(fqcn);
                if (clazz != null) {
                    final ClassLoader ldr = clazz.getClassLoader() != null ? clazz.getClassLoader() :
                        ClassLoader.getSystemClassLoader();
                    return locateContext(ldr, null);
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
                    return locateContext(Loader.loadClass(name).getClassLoader(), null);
                } catch (final ClassNotFoundException ex) {
                    //System.out.println("Could not load class " + name);
                }
            }
            final LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
            if (lc != null) {
                return lc;
            }
            return getDefault();
        }
    }

    public void removeContext(final LoggerContext context) {
        for (final Map.Entry<String, AtomicReference<WeakReference<LoggerContext>>> entry : CONTEXT_MAP.entrySet()) {
            final LoggerContext ctx = entry.getValue().get().get();
            if (ctx == context) {
                CONTEXT_MAP.remove(entry.getKey());
            }
        }
    }

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

    private LoggerContext locateContext(final ClassLoader loader, final String configLocation) {
        final String name = loader.toString();
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref == null) {
            LoggerContext ctx = new LoggerContext(name, null, configLocation);
            final AtomicReference<WeakReference<LoggerContext>> r =
                new AtomicReference<WeakReference<LoggerContext>>();
            r.set(new WeakReference<LoggerContext>(ctx));
            CONTEXT_MAP.putIfAbsent(loader.toString(), r);
            ctx = CONTEXT_MAP.get(name).get().get();
            return ctx;
        } else {
            final WeakReference<LoggerContext> r = ref.get();
            LoggerContext ctx = r.get();
            if (ctx != null) {
                return ctx;
            }
            ctx = new LoggerContext(name, null, configLocation);
            ref.compareAndSet(r, new WeakReference<LoggerContext>(ctx));
            return ctx;
        }
    }

    private static void setupCallerCheck() {
        try {
            final ClassLoader loader = Loader.getClassLoader();
            final Class clazz = loader.loadClass("sun.reflect.Reflection");
            final Method[] methods = clazz.getMethods();
            for (final Method method : methods) {
                final int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    break;
                }
            }
        } catch (final ClassNotFoundException cnfe) {
            LOGGER.debug("sun.reflect.Reflection is not installed");
        }
        try {
            securityManager = new PrivateSecurityManager();
        } catch (final Exception ex) {
            ex.printStackTrace();
            LOGGER.debug("Unable to install security manager", ex);
        }
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
     * SecurityManager that will locate the caller of the Log4j2 API.
     */
    private static class PrivateSecurityManager extends SecurityManager {

        public Class getCaller(final String fqcn) {
            final Class[] classes = getClassContext();
            boolean next = false;
            for (final Class clazz : classes) {
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

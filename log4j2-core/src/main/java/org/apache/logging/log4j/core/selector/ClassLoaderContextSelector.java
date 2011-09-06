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
import org.apache.logging.log4j.core.javaee.ContextAnchor;
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

    private static AtomicReference<LoggerContext> context = new AtomicReference<LoggerContext>();

    private static PrivateSecurityManager securityManager;

    private static Method getCallerClass;

    private static StatusLogger logger = StatusLogger.getLogger();

    private static ConcurrentMap<String, AtomicReference<WeakReference<LoggerContext>>> contextMap =
        new ConcurrentHashMap<String, AtomicReference<WeakReference<LoggerContext>>>();


    static {
        setupCallerCheck();
    }

    public LoggerContext getContext(String fqcn, boolean currentContext) {

        if (currentContext) {
            LoggerContext ctx = ContextAnchor.threadContext.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        } else {
            if (getCallerClass != null) {
                try {
                    Class clazz = Class.class;
                    boolean next = false;
                    for (int index = 2; clazz != null; ++index) {
                        Object[] params = new Object[] {index};
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
                } catch (Exception ex) {
                    // logger.debug("Unable to determine caller class via Sun Reflection", ex);
                }
            }

            if (securityManager != null) {
                Class clazz = securityManager.getCaller(fqcn);
                return locateContext(clazz.getClassLoader(), null);
            }

            Throwable t = new Throwable();
            boolean next = false;
            String name = null;
            for (StackTraceElement element : t.getStackTrace()) {
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
                } catch (ClassNotFoundException ex) {
                    //System.out.println("Could not load class " + name);
                }
            }
            LoggerContext lc = ContextAnchor.threadContext.get();
            if (lc != null) {
                return lc;
            }
            return getDefault();
        }
    }

    public void removeContext(LoggerContext context) {

        for (Map.Entry<String, AtomicReference<WeakReference<LoggerContext>>> entry : contextMap.entrySet()) {
            LoggerContext ctx = entry.getValue().get().get();
            if (ctx == context) {
                contextMap.remove(entry.getKey());
            }
        }
    }

    public List<LoggerContext> getLoggerContexts() {
        List<LoggerContext> list = new ArrayList<LoggerContext>();
        Collection<AtomicReference<WeakReference<LoggerContext>>> coll = contextMap.values();
        for (AtomicReference<WeakReference<LoggerContext>> ref : coll) {
            LoggerContext ctx = ref.get().get();
            if (ctx != null) {
                list.add(ctx);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private LoggerContext locateContext(ClassLoader loader, String configLocation) {
        String name = loader.toString();
        AtomicReference<WeakReference<LoggerContext>> ref = contextMap.get(name);
        if (ref == null) {
            LoggerContext ctx = new LoggerContext(name, null, configLocation);
            AtomicReference<WeakReference<LoggerContext>> r =
                new AtomicReference<WeakReference<LoggerContext>>();
            r.set(new WeakReference<LoggerContext>(ctx));
            contextMap.putIfAbsent(loader.toString(), r);
            ctx = contextMap.get(name).get().get();
            return ctx;
        } else {
            WeakReference<LoggerContext> r = ref.get();
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
            ClassLoader loader = Loader.getClassLoader();
            Class clazz = loader.loadClass("sun.reflect.Reflection");
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    break;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            logger.debug("sun.reflect.Reflection is not installed");
        }
        try {
            securityManager = new PrivateSecurityManager();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.debug("Unable to install security manager", ex);
        }
    }

    private LoggerContext getDefault() {
        LoggerContext ctx = context.get();
        if (ctx != null) {
            return ctx;
        }
        context.compareAndSet(null, new LoggerContext("Default"));
        return context.get();
    }

    private static class PrivateSecurityManager extends SecurityManager {

        public Class getCaller(String fqcn) {
            Class[] classes = getClassContext();
            boolean next = false;
            for (Class clazz : classes) {
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

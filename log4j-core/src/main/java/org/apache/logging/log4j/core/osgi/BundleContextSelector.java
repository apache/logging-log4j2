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
package org.apache.logging.log4j.core.osgi;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

/**
 * ContextSelector for OSGi bundles. This ContextSelector works rather similarly to the
 * {@link ClassLoaderContextSelector}, but instead of each ClassLoader having its own LoggerContext (like in a
 * servlet container), each OSGi bundle has its own LoggerContext.
 *
 * @since 2.1
 */
public class BundleContextSelector extends ClassLoaderContextSelector {

    @Override
    public void shutdown(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
        LoggerContext ctx = null;
        Bundle bundle = null;
        if (currentContext) {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
            ContextAnchor.THREAD_CONTEXT.remove();
        }
        if (ctx == null && loader instanceof BundleReference) {
            bundle = ((BundleReference) loader).getBundle();
            ctx = getLoggerContext(bundle);
            removeLoggerContext(ctx);
        }
        if (ctx == null) {
            final Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
            if (callerClass != null) {
                bundle = FrameworkUtil.getBundle(callerClass);
                ctx = getLoggerContext(FrameworkUtil.getBundle(callerClass));
                removeLoggerContext(ctx);
            }
        }
        if (ctx == null) {
            ctx = ContextAnchor.THREAD_CONTEXT.get();
            ContextAnchor.THREAD_CONTEXT.remove();
        }
        if (ctx != null) {
            ctx.stop(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        if (bundle != null && allContexts) {
            final Bundle[] bundles = bundle.getBundleContext().getBundles();
            for (final Bundle bdl : bundles) {
                ctx = getLoggerContext(bdl);
                if (ctx != null) {
                    ctx.stop(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private LoggerContext getLoggerContext(final Bundle bundle) {
        final String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref != null && ref.get() != null) {
            return ref.get().get();
        }
        return null;
    }

    private void removeLoggerContext(final LoggerContext context) {
        CONTEXT_MAP.remove(context.getName());
    }

    @Override
    public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        if (currentContext && ContextAnchor.THREAD_CONTEXT.get() != null) {
            return ContextAnchor.THREAD_CONTEXT.get().isStarted();
        }
        if (loader instanceof BundleReference) {
            return hasContext(((BundleReference) loader).getBundle());
        }
        final Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
        if (callerClass != null) {
            return hasContext(FrameworkUtil.getBundle(callerClass));
        }
        return ContextAnchor.THREAD_CONTEXT.get() != null
                && ContextAnchor.THREAD_CONTEXT.get().isStarted();
    }

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
        if (currentContext) {
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        }
        // it's quite possible that the provided ClassLoader may implement BundleReference which gives us a nice
        // shortcut
        if (loader instanceof BundleReference) {
            return locateContext(((BundleReference) loader).getBundle(), configLocation);
        }
        final Class<?> callerClass = StackLocatorUtil.getCallerClass(fqcn);
        if (callerClass != null) {
            return locateContext(FrameworkUtil.getBundle(callerClass), configLocation);
        }
        final LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
        return lc == null ? getDefault() : lc;
    }

    private static boolean hasContext(final Bundle bundle) {
        final String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        return ref != null
                && ref.get() != null
                && ref.get().get() != null
                && ref.get().get().isStarted();
    }

    private static LoggerContext locateContext(final Bundle bundle, final URI configLocation) {
        final String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref == null) {
            final LoggerContext context = new LoggerContext(name, bundle, configLocation);
            CONTEXT_MAP.putIfAbsent(name, new AtomicReference<>(new WeakReference<>(context)));
            return CONTEXT_MAP.get(name).get().get();
        }
        final WeakReference<LoggerContext> r = ref.get();
        final LoggerContext ctx = r.get();
        if (ctx == null) {
            final LoggerContext context = new LoggerContext(name, bundle, configLocation);
            ref.compareAndSet(r, new WeakReference<>(context));
            return ref.get().get();
        }
        final URI oldConfigLocation = ctx.getConfigLocation();
        if (oldConfigLocation == null && configLocation != null) {
            LOGGER.debug("Setting bundle ({}) configuration to {}", name, configLocation);
            ctx.setConfigLocation(configLocation);
        } else if (oldConfigLocation != null && configLocation != null && !configLocation.equals(oldConfigLocation)) {
            LOGGER.warn(
                    "locateContext called with URI [{}], but existing LoggerContext has URI [{}]",
                    configLocation,
                    oldConfigLocation);
        }
        return ctx;
    }
}

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
package org.apache.logging.log4j.core.osgi;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.util.ReflectionUtil;
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
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
                                    final URI configLocation) {
        if (currentContext) {
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            if (ctx != null) {
                return ctx;
            }
            return getDefault();
        }
        // it's quite possible that the provided ClassLoader may implement BundleReference which gives us a nice shortcut
        if (loader instanceof BundleReference) {
            return locateContext(((BundleReference) loader).getBundle(), configLocation);
        }
        final Class<?> callerClass = ReflectionUtil.getCallerClass(fqcn);
        if (callerClass != null) {
            return locateContext(FrameworkUtil.getBundle(callerClass), configLocation);
        }
        final LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
        return lc == null ? getDefault() : lc;
    }

    private static LoggerContext locateContext(final Bundle bundle, final URI configLocation) {
        final String name = Objects.requireNonNull(bundle, "No Bundle provided").getSymbolicName();
        final AtomicReference<WeakReference<LoggerContext>> ref = CONTEXT_MAP.get(name);
        if (ref == null) {
            final LoggerContext context = new LoggerContext(name, bundle, configLocation);
            CONTEXT_MAP.putIfAbsent(name,
                new AtomicReference<>(new WeakReference<>(context)));
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
            LOGGER.warn("locateContext called with URI [{}], but existing LoggerContext has URI [{}]",
                configLocation, oldConfigLocation);
        }
        return ctx;
    }
}

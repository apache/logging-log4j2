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
package org.apache.logging.log4j.core.async;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * Returns either this Thread's context or the default {@link AsyncLoggerContext}.
 * Single-application instances should prefer this implementation over the {@link AsyncLoggerContextSelector}
 * due to the reduced overhead avoiding classloader lookups.
 */
public class BasicAsyncLoggerContextSelector implements ContextSelector {

    private static final AsyncLoggerContext CONTEXT = new AsyncLoggerContext("AsyncDefault");

    @Override
    public void shutdown(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
        final LoggerContext ctx = getContext(fqcn, loader, currentContext);
        if (ctx != null && ctx.isStarted()) {
            ctx.stop(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        final LoggerContext ctx = getContext(fqcn, loader, currentContext);
        return ctx != null && ctx.isStarted();
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        return ctx != null ? ctx : CONTEXT;
    }

    @Override
    public LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        return ctx != null ? ctx : CONTEXT;
    }

    @Override
    public void removeContext(final LoggerContext context) {
        // does not remove anything
    }

    @Override
    public boolean isClassLoaderDependent() {
        return false;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        return Collections.singletonList(CONTEXT);
    }
}

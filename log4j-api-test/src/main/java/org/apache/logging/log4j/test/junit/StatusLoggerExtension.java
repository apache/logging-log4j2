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
package org.apache.logging.log4j.test.junit;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;

class StatusLoggerExtension extends TypeBasedParameterResolver<ListStatusListener>
        implements BeforeAllCallback, BeforeEachCallback, TestExecutionExceptionHandler {

    private static final Object KEY = ListStatusListener.class;

    public StatusLoggerExtension() {
        super(ListStatusListener.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Stores the per-class status listener to catch the messages caused by other
        // `beforeAll` methods and extensions.
        final ListStatusListenerHolder holder = new ListStatusListenerHolder(context, null);
        ExtensionContextAnchor.setAttribute(KEY, holder, context);
        ReflectionSupport.findFields(
                        context.getRequiredTestClass(),
                        f -> ModifierSupport.isStatic(f) && f.getType().equals(ListStatusListener.class),
                        HierarchyTraversalMode.TOP_DOWN)
                .forEach(f -> {
                    try {
                        f.setAccessible(true);
                        f.set(null, holder.getStatusListener());
                    } catch (final ReflectiveOperationException e) {
                        throw new ExtensionContextException("Failed to inject field.", e);
                    }
                });
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        // Retrieves the per-class status listener
        final ListStatusListenerHolder parentHolder =
                ExtensionContextAnchor.getAttribute(KEY, ListStatusListenerHolder.class, context);
        final ListStatusListener parent = parentHolder != null ? parentHolder.getStatusListener() : null;
        final ListStatusListenerHolder holder = new ListStatusListenerHolder(context, parent);
        ExtensionContextAnchor.setAttribute(KEY, holder, context);
        ReflectionSupport.findFields(
                        context.getRequiredTestClass(),
                        f -> ModifierSupport.isNotStatic(f) && f.getType().equals(ListStatusListener.class),
                        HierarchyTraversalMode.TOP_DOWN)
                .forEach(f -> {
                    try {
                        f.setAccessible(true);
                        f.set(context.getRequiredTestInstance(), holder.getStatusListener());
                    } catch (final ReflectiveOperationException e) {
                        throw new ExtensionContextException("Failed to inject field.", e);
                    }
                });
    }

    @Override
    public void handleTestExecutionException(final ExtensionContext context, final Throwable throwable)
            throws Throwable {
        final ListStatusListenerHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, ListStatusListenerHolder.class, context);
        if (holder != null) {
            holder.handleException(context, throwable);
        }
        throw throwable;
    }

    @Override
    public ListStatusListener resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final ListStatusListenerHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, ListStatusListenerHolder.class, extensionContext);
        return holder.getStatusListener();
    }

    private static class ListStatusListenerHolder implements CloseableResource {

        private final StatusLogger statusLogger;
        private final ListStatusListener statusListener;

        public ListStatusListenerHolder(final ExtensionContext context, final ListStatusListener parent) {
            this.statusLogger = StatusLogger.getLogger();
            this.statusListener = new JUnitListStatusListener(context, parent);
            statusLogger.registerListener(statusListener);
        }

        public ListStatusListener getStatusListener() {
            return statusListener;
        }

        @Override
        public void close() throws Throwable {
            statusLogger.removeListener(statusListener);
        }

        public void handleException(final ExtensionContext context, final Throwable throwable) {
            final Logger logger = new SimpleLogger(
                    "StatusLoggerExtension",
                    Level.ALL,
                    false,
                    false,
                    false,
                    false,
                    null,
                    ParameterizedNoReferenceMessageFactory.INSTANCE,
                    PropertiesUtil.getProperties(),
                    System.err);
            logger.error("Test {} failed.\nDumping status data:", context.getDisplayName());
            final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME.withZone(ZoneId.systemDefault());
            statusListener.getStatusData().forEach(data -> {
                logger.atLevel(data.getLevel())
                        .withThrowable(data.getThrowable())
                        .withLocation(data.getStackTraceElement())
                        .log("{} {}", formatter.format(Instant.ofEpochMilli(data.getTimestamp())), data.getMessage());
            });
        }
    }

    private static class JUnitListStatusListener implements ListStatusListener {

        private final ExtensionContext context;
        private final ListStatusListener parent;
        private final ArrayList<StatusData> statusData = new ArrayList<>();

        public JUnitListStatusListener(final ExtensionContext context, final ListStatusListener parent) {
            this.context = context;
            this.parent = parent;
        }

        @Override
        public void log(final StatusData data) {
            if (context.equals(ExtensionContextAnchor.getContext())) {
                synchronized (statusData) {
                    statusData.add(data);
                }
            }
        }

        @Override
        public Level getStatusLevel() {
            return Level.DEBUG;
        }

        @Override
        public void close() throws IOException {
            // NOP
        }

        @Override
        public Stream<StatusData> getStatusData() {
            synchronized (statusData) {
                final List<StatusData> clone = (List<StatusData>) statusData.clone();
                return parent != null ? Stream.concat(parent.getStatusData(), clone.stream()) : clone.stream();
            }
        }

        @Override
        public void clear() {
            synchronized (statusData) {
                statusData.clear();
            }
        }
    }
}

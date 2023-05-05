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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.ListStatusListener;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

class StatusLoggerExtension extends TypeBasedParameterResolver<ListStatusListener>
        implements BeforeEachCallback, TestExecutionExceptionHandler {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final StatusConsoleListener CONSOLE_LISTENER = new StatusConsoleListener(Level.ALL);
    private static final Object KEY = ListStatusListener.class;

    public StatusLoggerExtension() {
        super(ListStatusListener.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final ListStatusListenerHolder holder = new ListStatusListenerHolder(context);
        ExtensionContextAnchor.setAttribute(KEY, holder, context);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        final ListStatusListener statusListener = resolveParameter(null, context);
        statusListener.getStatusData().forEach(CONSOLE_LISTENER::log);
        throw throwable;
    }

    @Override
    public ListStatusListener resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final ListStatusListenerHolder holder = ExtensionContextAnchor.getAttribute(KEY, ListStatusListenerHolder.class,
                extensionContext);
        return holder.getStatusListener();
    }

    private static class ListStatusListenerHolder implements CloseableResource {

        private final ListStatusListener statusListener;

        public ListStatusListenerHolder(ExtensionContext context) {
            this.statusListener = new JUnitListStatusListener(context);
            LOGGER.registerListener(statusListener);
        }

        public ListStatusListener getStatusListener() {
            return statusListener;
        }

        @Override
        public void close() throws Throwable {
            LOGGER.removeListener(statusListener);
        }

    }

    private static class JUnitListStatusListener implements ListStatusListener {

        private final ExtensionContext context;
        private final List<StatusData> statusData = Collections.synchronizedList(new ArrayList<StatusData>());

        public JUnitListStatusListener(ExtensionContext context) {
            this.context = context;
        }

        @Override
        public void log(StatusData data) {
            if (context.equals(ExtensionContextAnchor.getContext())) {
                statusData.add(data);
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
            return statusData.stream();
        }

    }
}

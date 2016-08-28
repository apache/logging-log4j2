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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;

/**
 * Returns either this Thread's context or the default LoggerContext.
 */
public class BasicContextSelector implements ContextSelector {

    private static final LoggerContext CONTEXT = new LoggerContext("Default");

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {

        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        return ctx != null ? ctx : CONTEXT;
    }


    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
                                    final URI configLocation) {

        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        return ctx != null ? ctx : CONTEXT;
    }

    public LoggerContext locateContext(final String name, final String configLocation) {
        return CONTEXT;
    }

    @Override
    public void removeContext(final LoggerContext context) {
        // does not remove anything
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        final List<LoggerContext> list = new ArrayList<>();
        list.add(CONTEXT);
        return Collections.unmodifiableList(list);
    }

}

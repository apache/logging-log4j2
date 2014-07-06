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
package org.apache.logging.log4j.core.async;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * {@code ContextSelector} that returns the singleton {@code AsyncLoggerContext}.
 */
public class AsyncLoggerContextSelector implements ContextSelector {

    // LOG4J2-666 ensure unique name across separate instances created by webapp classloaders
    private static final AsyncLoggerContext CONTEXT = new AsyncLoggerContext("AsyncLoggerContext@"
            + AsyncLoggerContext.class.hashCode());

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        return CONTEXT;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        final List<LoggerContext> list = new ArrayList<LoggerContext>();
        list.add(CONTEXT);
        return Collections.unmodifiableList(list);
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
            final URI configLocation) {
        return CONTEXT;
    }

    @Override
    public void removeContext(final LoggerContext context) {
    }

}

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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * {@code ContextSelector} that returns the singleton {@code AsyncLoggerContext}.
 */
public class AsyncLoggerContextSelector implements ContextSelector {

    private ConcurrentMap<String, AsyncLoggerContext> contexts = new ConcurrentHashMap<String, AsyncLoggerContext>();

    /**
     * Returns {@code true} if the user specified this selector as the Log4jContextSelector, to make all loggers
     * asynchronous.
     * 
     * @return {@code true} if all loggers are asynchronous, {@code false} otherwise.
     */
    public static boolean isSelected() {
        return AsyncLoggerContextSelector.class.getName().equals(
                PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_CONTEXT_SELECTOR));
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        // LOG4J2-666 ensure unique name across separate instances created by webapp classloaders
        final int hash = loader == null ? getClass().getClassLoader().hashCode() : loader.hashCode();
        final String key = "AsyncLoggerContext@" + Integer.toHexString(hash);
        AsyncLoggerContext result = contexts.get(key);
        if (result == null) {
            result = new AsyncLoggerContext(key);
            return contexts.putIfAbsent(key, result);
        }
        return result;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        return new ArrayList<LoggerContext>(contexts.values());
    }

    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
            final URI configLocation) {
        return getContext(fqcn, loader, currentContext);
    }

    @Override
    public void removeContext(final LoggerContext context) {
        contexts.remove(context.getName());
    }

}

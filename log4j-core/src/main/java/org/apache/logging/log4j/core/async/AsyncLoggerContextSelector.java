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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * {@code ContextSelector} that manages {@code AsyncLoggerContext} instances.
 * <p>
 * As of version 2.5, this class extends ClassLoaderContextSelector for better web app support.
 */
public class AsyncLoggerContextSelector extends ClassLoaderContextSelector {

    /**
     * Returns {@code true} if the user specified this selector as the Log4jContextSelector, to make all loggers
     * asynchronous.
     *
     * @return {@code true} if all loggers are asynchronous, {@code false} otherwise.
     */
    public static boolean isSelected() {
        return AsyncLoggerContextSelector.class
                .getName()
                .equals(PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_CONTEXT_SELECTOR));
    }

    @Override
    protected LoggerContext createContext(final String name, final URI configLocation) {
        return new AsyncLoggerContext(name, null, configLocation);
    }

    @Override
    protected String toContextMapKey(final ClassLoader loader) {
        // LOG4J2-666 ensure unique name across separate instances created by webapp classloaders
        return "AsyncContext@" + Integer.toHexString(System.identityHashCode(loader));
    }

    @Override
    protected String defaultContextName() {
        return "DefaultAsyncContext@" + Thread.currentThread().getName();
    }
}

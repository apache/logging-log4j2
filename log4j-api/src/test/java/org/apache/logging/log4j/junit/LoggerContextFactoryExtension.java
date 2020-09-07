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

package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Field;
import java.util.List;

class LoggerContextFactoryExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String KEY = "previousFactory";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final List<Field> loggerContextFactories = ReflectionSupport.findFields(testClass,
                f -> ModifierSupport.isStatic(f) && f.isAnnotationPresent(RegisterLoggerContextFactory.class),
                HierarchyTraversalMode.BOTTOM_UP);
        if (loggerContextFactories.isEmpty()) {
            return;
        }
        if (loggerContextFactories.size() > 1) {
            throw new IllegalArgumentException("More than one static LoggerContextFactory specified in " + testClass.getName());
        }
        getStore(context).put(KEY, LogManager.getFactory());
        final LoggerContextFactory factory = (LoggerContextFactory) loggerContextFactories.get(0).get(null);
        LogManager.setFactory(factory);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        LogManager.setFactory(getStore(context).get(KEY, LoggerContextFactory.class));
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass()));
    }
}

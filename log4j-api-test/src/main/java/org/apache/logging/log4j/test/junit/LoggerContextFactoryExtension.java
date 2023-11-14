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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that sets a particular {@link LoggerContextFactory} for the entire run of tests in a class.
 *
 * @since 2.14.0
 */
public class LoggerContextFactoryExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String KEY = "previousFactory";
    private final LoggerContextFactory loggerContextFactory;

    public LoggerContextFactoryExtension(final LoggerContextFactory loggerContextFactory) {
        this.loggerContextFactory = loggerContextFactory;
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        getStore(context).put(KEY, LogManager.getFactory());
        LogManager.setFactory(loggerContextFactory);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        LogManager.setFactory(getStore(context).get(KEY, LoggerContextFactory.class));
    }

    private ExtensionContext.Store getStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestClass()));
    }
}

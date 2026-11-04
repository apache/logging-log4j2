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
package org.apache.logging.log4j.core.test.junit;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class CleanFoldersRuleExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private final String CONFIG;
    private final String ClassName;
    private final ClassLoader ClassNameLoader;
    private LoggerContext context;
    private CleanFolders cleanFolders;

    public CleanFoldersRuleExtension(
            final String DIR, final String CONFIG, final String ClassName, final ClassLoader ClassNameLoader) {
        this.CONFIG = CONFIG;
        this.ClassName = ClassName;
        this.ClassNameLoader = ClassNameLoader;
        this.cleanFolders = new CleanFolders(DIR);
    }

    public CleanFoldersRuleExtension(
            final String DIR,
            final String CONFIG,
            final String ClassName,
            final ClassLoader ClassNameLoader,
            final boolean before,
            final boolean after,
            final int maxTries) {
        this.CONFIG = CONFIG;
        this.ClassName = ClassName;
        this.ClassNameLoader = ClassNameLoader;
        this.cleanFolders = new CleanFolders(before, after, maxTries, DIR);
    }

    @Override
    public void beforeEach(final ExtensionContext ctx) throws Exception {
        this.cleanFolders.beforeEach(ctx);
        this.context = Configurator.initialize(ClassName, ClassNameLoader, CONFIG);
    }

    @Override
    public void afterEach(final ExtensionContext ctx) throws Exception {
        if (this.context != null) {
            Configurator.shutdown(this.context, 10, TimeUnit.SECONDS);
            StatusLogger.getLogger().reset();
        }
        this.cleanFolders.afterEach(ctx);
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        // Check if the parameter is of type LoggerContext
        return parameterContext.getParameter().getType().equals(LoggerContext.class);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        // Return the LoggerContext instance
        return this.context;
    }
}

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
package org.apache.logging.slf4j;

import ch.qos.logback.classic.Logger;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.apache.logging.slf4j.LoggerContextResolver.LoggerContextHolder;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;

public class LoggerResolver extends TypeBasedParameterResolver<Logger>
        implements BeforeAllCallback, BeforeEachCallback {

    private static final Object KEY = LoggerContextHolder.class;

    @Override
    public Logger resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext)
                .getLogger();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        final LoggerContextHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext);
        if (holder != null) {
            ReflectionSupport.findFields(
                            extensionContext.getRequiredTestClass(),
                            f -> ModifierSupport.isStatic(f) && f.getType().equals(Logger.class),
                            HierarchyTraversalMode.TOP_DOWN)
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            f.set(null, holder.getLogger());
                        } catch (ReflectiveOperationException e) {
                            throw new ExtensionContextException("Failed to inject field " + f, e);
                        }
                    });
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        final LoggerContextHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext);
        if (holder != null) {
            ReflectionSupport.findFields(
                            extensionContext.getRequiredTestClass(),
                            f -> ModifierSupport.isNotStatic(f) && f.getType().equals(Logger.class),
                            HierarchyTraversalMode.TOP_DOWN)
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            f.set(extensionContext.getRequiredTestInstance(), holder.getLogger());
                        } catch (ReflectiveOperationException e) {
                            throw new ExtensionContextException("Failed to inject field " + f, e);
                        }
                    });
        }
    }
}

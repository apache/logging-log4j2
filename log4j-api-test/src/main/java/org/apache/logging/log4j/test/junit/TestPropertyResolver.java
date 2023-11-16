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

import org.apache.logging.log4j.test.TestProperties;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;

public class TestPropertyResolver extends TypeBasedParameterResolver<TestProperties>
        implements BeforeAllCallback, BeforeEachCallback {

    public TestPropertyResolver() {
        super(TestProperties.class);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final TestProperties props = TestPropertySource.createProperties(context);
        AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestMethod(), SetTestProperty.class)
                .forEach(setProperty -> props.setProperty(setProperty.key(), setProperty.value()));
        final Class<?> testClass = context.getRequiredTestClass();
        final Object testInstance = context.getRequiredTestInstance();
        ReflectionSupport.findFields(
                        testClass,
                        field -> ModifierSupport.isNotStatic(field)
                                && field.getType().equals(TestProperties.class),
                        HierarchyTraversalMode.BOTTOM_UP)
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        field.set(testInstance, props);
                    } catch (IllegalAccessException e) {
                        throw new UnsupportedOperationException(e);
                    }
                });
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final TestProperties props = TestPropertySource.createProperties(context);
        AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestClass(), SetTestProperty.class)
                .forEach(setProperty -> props.setProperty(setProperty.key(), setProperty.value()));
        final Class<?> testClass = context.getRequiredTestClass();
        ReflectionSupport.findFields(
                        testClass,
                        field -> ModifierSupport.isStatic(field)
                                && field.getType().equals(TestProperties.class),
                        HierarchyTraversalMode.BOTTOM_UP)
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        field.set(null, props);
                    } catch (IllegalAccessException e) {
                        throw new UnsupportedOperationException(e);
                    }
                });
    }

    @Override
    public TestProperties resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return TestPropertySource.createProperties(extensionContext);
    }
}

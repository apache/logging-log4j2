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

import java.lang.reflect.Modifier;
import java.util.Properties;

import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.test.TestPropertySource;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

public class TestPropertyResolver extends TypeBasedParameterResolver<Properties>
        implements BeforeAllCallback, BeforeEachCallback {

    private static final Namespace CLASS_NAMESPACE = Namespace.create(TestPropertyResolver.class);
    private static final Namespace INSTANCE_NAMESPACE = CLASS_NAMESPACE.append("INSTANCE");
    private static final String KEY = "properties";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final SetTestProperty[] setProperties = context.getRequiredTestMethod()
                .getAnnotationsByType(SetTestProperty.class);
        if (setProperties.length > 0) {
            final Properties props = getProperties(context);
            for (final SetTestProperty setProperty : setProperties) {
                props.setProperty(setProperty.key(), setProperty.value());
            }
        }
        final Class<?> testClass = context.getRequiredTestClass();
        Object testInstance = context.getRequiredTestInstance();
        AnnotationUtils.findAnnotatedFields(testClass, TestProperties.class,
                field -> !Modifier.isStatic(field.getModifiers()))
                .forEach(field -> {
                    final Properties props = getProperties(context);
                    ReflectionUtil.setFieldValue(field, testInstance, props);
                });
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final SetTestProperty[] setProperties = context.getRequiredTestClass()
                .getAnnotationsByType(SetTestProperty.class);
        if (setProperties.length > 0) {
            final Properties props = getProperties(context);
            for (final SetTestProperty setProperty : setProperties) {
                props.setProperty(setProperty.key(), setProperty.value());
            }
        }
        final Class<?> testClass = context.getRequiredTestClass();
        AnnotationUtils.findAnnotatedFields(testClass, TestProperties.class,
                field -> Modifier.isStatic(field.getModifiers()))
                .forEach(field -> {
                    final Properties props = getProperties(context);
                    ReflectionUtil.setStaticFieldValue(field, props);
                });
    }

    static Properties getProperties(ExtensionContext context) {
        final Namespace namespace = context.getTestInstance().isPresent() ? INSTANCE_NAMESPACE : CLASS_NAMESPACE;
        return context.getStore(namespace)
                .getOrComputeIfAbsent(KEY, unused -> {
                    final Properties parent = TestPropertySource.peek();
                    final PropertiesHolder holder = new PropertiesHolder(parent);
                    TestPropertySource.push(holder.getProperties());
                    return holder;
                }, PropertiesHolder.class)
                .getProperties();
    }

    private static class PropertiesHolder implements CloseableResource {

        private final Properties properties;

        public PropertiesHolder(final Properties defaults) {
            this.properties = new Properties(defaults);
        }

        public Properties getProperties() {
            return properties;
        }

        @Override
        public void close() throws Throwable {
            Properties properties = TestPropertySource.pop();
            if (!this.properties.equals(properties)) {
                throw new ExtensionContextException("Test properties corruption.");
            }
        }

    }

    @Override
    public Properties resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return getProperties(extensionContext);
    }
}

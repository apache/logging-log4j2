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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

public class PrivateDirectoryResolver extends TypeBasedParameterResolver<Path>
        implements BeforeAllCallback, BeforeEachCallback {

    private static final String KEY = "privateDir";
    private static final Path PARENT_PATH = Paths.get("target", KEY);
    private static final Namespace NAMESPACE = Namespace.create(PrivateDirectoryResolver.class);

    @Override
    public Path resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return getPrivateDirectory(extensionContext);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        AnnotationUtils.findAnnotatedFields(testClass, PrivateDir.class,
                field -> Modifier.isStatic(field.getModifiers()))
                .forEach(field -> {
                    final Path privateDir = getPrivateDirectory(context);
                    ReflectionUtil.setStaticFieldValue(field, privateDir);
                });
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final Object testInstance = context.getRequiredTestInstance();
        AnnotationUtils.findAnnotatedFields(testClass, PrivateDir.class,
                field -> !Modifier.isStatic(field.getModifiers()))
                .forEach(field -> {
                    final Path privateDir = getPrivateDirectory(context);
                    ReflectionUtil.setFieldValue(field, testInstance, privateDir);
                });
    }

    private Path getPrivateDirectory(ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .getOrComputeIfAbsent(KEY, unused -> {
                    final PathHolder holder = createPrivateDirectory(context.getRequiredTestClass());
                    TestPropertyResolver.getProperties(context)
                            .setProperty(KEY, holder.getPath().toString());
                    return holder;
                }, PathHolder.class)
                .getPath();
    }

    private PathHolder createPrivateDirectory(Class<?> testClass) {
        try {
            Files.createDirectories(PARENT_PATH);
            return new PathHolder(Files.createTempDirectory(PARENT_PATH, testClass.getName()));
        } catch (IOException e) {
            throw new ExtensionConfigurationException("Failed to create private directory", e);
        }
    }

    private static class PathHolder implements CloseableResource {

        private final Path path;

        public PathHolder(Path path) {
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public void close() throws Throwable {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

    }

}

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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.TestProperties;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ModifierSupport;

import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS;

public class TempLoggingDirectory implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final List<Field> fields = AnnotationSupport.findAnnotatedFields(context.getRequiredTestClass(),
                TempLoggingDir.class, ModifierSupport::isStatic);
        Path loggingPath = null;
        for (final Field field : fields) {
            if (loggingPath != null) {
                LOGGER.warn("Multiple fields with @TempLoggingDir annotation are not supported.");
            } else {
                final CleanupMode cleanup = determineCleanupMode(field);
                loggingPath = createLoggingPath(context, cleanup).getPath();
            }
            field.setAccessible(true);
            field.set(null, loggingPath);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // JUnit 5 does not set an error on the parent context if one of the children
        // fail. We record the list of children.
        final PathHolder holder = ExtensionContextAnchor.getAttribute(PathHolder.class, PathHolder.class, context);
        if (holder != null) {
            holder.addContext(context);
        }
    }

    private PathHolder createLoggingPath(final ExtensionContext context, final CleanupMode cleanup) throws IOException {
        final TestProperties props = TestPropertySource.createProperties(context);
        // Create temporary directory
        final String baseDir = System.getProperty("basedir");
        final Path basePath = (baseDir != null ? Paths.get(baseDir, "target") : Paths.get(".")).resolve("logs");
        final Class<?> clazz = context.getRequiredTestClass();
        final String dir = clazz.getName().replaceAll("[.$]", File.separatorChar == '\\' ? "\\\\" : File.separator);
        final Path loggingPath = basePath.resolve(dir);
        Files.createDirectories(loggingPath);
        props.setProperty(TestProperties.LOGGING_PATH, loggingPath.toString());
        // Register deletion
        final PathHolder holder = new PathHolder(loggingPath, cleanup, context);
        ExtensionContextAnchor.setAttribute(PathHolder.class, holder, context);
        return holder;
    }

    private CleanupMode determineCleanupMode(final TempLoggingDir annotation) {
        final CleanupMode mode = annotation.cleanup();
        // TODO: use JupiterConfiguration
        return mode != CleanupMode.DEFAULT ? mode : CleanupMode.ON_SUCCESS;
    }

    private CleanupMode determineCleanupMode(final Field field) {
        return determineCleanupMode(field.getAnnotation(TempLoggingDir.class));
    }

    private static class PathHolder implements CloseableResource {

        private final Path path;
        private final CleanupMode cleanupMode;
        private final Map<ExtensionContext, Boolean> contexts = new ConcurrentHashMap<>();

        public PathHolder(final Path path, final CleanupMode cleanup, final ExtensionContext context) {
            this.path = path;
            this.cleanupMode = cleanup;
            this.contexts.put(context, Boolean.TRUE);
        }

        public void addContext(final ExtensionContext context) {
            this.contexts.put(context, Boolean.TRUE);
        }

        public Path getPath() {
            return path;
        }

        @Override
        public void close() throws IOException {
            if (cleanupMode == NEVER || (cleanupMode == ON_SUCCESS
                    && contexts.keySet().stream().anyMatch(context -> context.getExecutionException().isPresent()))) {
                LOGGER.debug("Skipping cleanup of directory {}.", path);
                return;
            }
            DirectoryCleaner.deleteDirectory(path);
        }

    }
}

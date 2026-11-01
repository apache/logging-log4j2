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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Plugin
@Namespace(CompressActionFactoryProvider.NAMESPACE)
@Ordered(Ordered.LAST)
@NullMarked
public class JreCompressActionFactoryProvider implements CompressActionFactoryProvider {

    @Override
    public @Nullable CompressActionFactory createFactoryForAlgorithm(String extension) {
        return switch (extension) {
            case GzCompressActionFactory.NAME -> new GzCompressActionFactory();
            case ZipCompressActionFactory.NAME -> new ZipCompressActionFactory();
            default -> null;
        };
    }

    private abstract static class DeflaterCompressActionFactory implements CompressActionFactory {

        private static final Logger LOGGER = StatusLogger.getLogger();

        private final String name;
        private final Set<String> warned = ConcurrentHashMap.newKeySet();

        DeflaterCompressActionFactory(final String name) {
            this.name = name;
        }

        abstract Action createCompressAction(Path source, Path destination, int compressionLevel);

        @Override
        public Action createCompressAction(
                final Path source, final Path destination, final Map<String, String> options) {
            return createCompressAction(source, destination, resolveCompressionLevel(options));
        }

        private int resolveCompressionLevel(final Map<String, String> options) {
            final String value = options.get(COMPRESSION_LEVEL);
            if (value == null) {
                return Deflater.DEFAULT_COMPRESSION;
            }
            try {
                final int level = Integer.parseInt(value.trim());
                // `Deflater` validates the level
                final Deflater deflater = new Deflater();
                try {
                    deflater.setLevel(level);
                } finally {
                    deflater.end();
                }
                return level;
            } catch (final IllegalArgumentException e) {
                if (warned.add(value)) {
                    LOGGER.warn(
                            "Unsupported {} compression level '{}' ({}); using the default level.",
                            name,
                            value,
                            e.getMessage());
                }
                return Deflater.DEFAULT_COMPRESSION;
            }
        }

        @Override
        public String getAlgorithmName() {
            return name;
        }
    }

    private static final class GzCompressActionFactory extends DeflaterCompressActionFactory {

        static final String NAME = "gz";

        GzCompressActionFactory() {
            super(NAME);
        }

        @Override
        Action createCompressAction(final Path source, final Path destination, final int compressionLevel) {
            return new GzCompressAction(source, destination, compressionLevel);
        }
    }

    private static final class ZipCompressActionFactory extends DeflaterCompressActionFactory {

        static final String NAME = "zip";

        ZipCompressActionFactory() {
            super(NAME);
        }

        @Override
        Action createCompressAction(final Path source, final Path destination, final int compressionLevel) {
            return new ZipCompressAction(source, destination, compressionLevel);
        }
    }
}

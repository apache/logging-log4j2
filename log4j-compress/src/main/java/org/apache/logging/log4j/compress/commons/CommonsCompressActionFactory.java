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
package org.apache.logging.log4j.compress.commons;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class CommonsCompressActionFactory implements CompressActionFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final CompressorStreamProvider provider;

    private final String name;

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    CommonsCompressActionFactory(final String name, final CompressorStreamProvider provider) {
        this.name = name;
        this.provider = provider;
    }

    @Override
    public Action createCompressAction(final Path source, final Path destination, final Map<String, String> options) {
        final String value = options.get(COMPRESSION_LEVEL);
        final Integer compressionLevel = value != null ? resolveCompressionLevel(value) : null;
        return new CommonsCompressAction(provider, name, source, destination, compressionLevel);
    }

    /**
     * Returns the level or {@code null}, if the level can not be used. The warning is logged once per value.
     */
    private @Nullable Integer resolveCompressionLevel(final String value) {
        if (!LevelAwareCompressors.isSupported(name)) {
            warnOnce(value, "The {} algorithm does not support compression levels; ignoring '{}'.", name, value);
            return null;
        }
        try {
            final int level = Integer.parseInt(value.trim());
            LevelAwareCompressors.validate(name, level);
            return level;
        } catch (final IllegalArgumentException e) {
            warnOnce(
                    value,
                    "Unsupported {} compression level '{}' ({}); using the default level.",
                    name,
                    value,
                    e.getMessage());
            return null;
        }
    }

    private void warnOnce(final String key, final String message, final Object... parameters) {
        if (warned.add(key)) {
            LOGGER.warn(message, parameters);
        }
    }

    @Override
    public String getAlgorithmName() {
        return name;
    }
}

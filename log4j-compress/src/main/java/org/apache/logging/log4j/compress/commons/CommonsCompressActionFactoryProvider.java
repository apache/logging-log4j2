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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.Nullable;

@Plugin
@Namespace(CompressActionFactoryProvider.NAMESPACE)
@Ordered(0)
public final class CommonsCompressActionFactoryProvider implements CompressActionFactoryProvider {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String COMMONS_COMPRESS_DOCUMENTATION =
            "See https://commons.apache.org/proper/commons-compress/index.html for more information.";

    private final CompressorStreamFactory factory = new CompressorStreamFactory();

    private static void missingDependencyWarning(final String algorithm) {
        LOGGER.warn(
                "{} compression is not available due to a missing dependency. {}",
                algorithm,
                COMMONS_COMPRESS_DOCUMENTATION);
    }

    private @Nullable CompressorStreamProvider getCompressorStreamProvider(String algorithm) {
        switch (toRootLowerCase(algorithm)) {
            case CompressorStreamFactory.LZMA:
                if (!LZMAUtils.isLZMACompressionAvailable()) {
                    missingDependencyWarning("LZMA");
                    return null;
                }
                break;
            case CompressorStreamFactory.PACK200:
                LOGGER.warn("Pack200 compression is not suitable for log files and will not be used.");
                return null;
            case CompressorStreamFactory.XZ:
                if (!XZUtils.isXZCompressionAvailable()) {
                    missingDependencyWarning("XZ");
                    return null;
                }
                break;
            case CompressorStreamFactory.ZSTANDARD:
                if (!ZstdUtils.isZstdCompressionAvailable()) {
                    missingDependencyWarning("Zstd");
                    return null;
                }
                break;
        }
        // Commons Compress uses upper case keys.
        return CompressorStreamFactory.findAvailableCompressorOutputStreamProviders()
                .get(toRootUpperCase(algorithm));
    }

    @Override
    public @Nullable CompressActionFactory createFactoryForAlgorithm(String algorithm) {
        CompressorStreamProvider provider = getCompressorStreamProvider(algorithm);
        if (provider != null) {
            return new CommonsCompressActionFactory(algorithm, provider);
        }
        return null;
    }
}

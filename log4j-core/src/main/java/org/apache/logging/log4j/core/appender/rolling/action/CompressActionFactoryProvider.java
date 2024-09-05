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
package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.core.appender.rolling.action.internal.CompositeCompressActionFactoryProvider;
import org.apache.logging.log4j.core.config.Configuration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface for plugins that provide additional compression algorithms.
 *
 * @since 3.0.0
 */
@NullMarked
public interface CompressActionFactoryProvider {

    String NAMESPACE = "compress";

    /**
     * Creates the appropriate {@link CompressActionFactory} for the given compression algorithm.
     * <p>
     *     When applicable the algorithm should correspond to the name used by Apache Commons Compress.
     * </p>
     * @param algorithm The compression algorithm.
     * @return A {@link CompressActionFactory} or {@code null} if the extension is not supported.
     */
    @Nullable
    CompressActionFactory createFactoryForAlgorithm(String algorithm);

    /**
     * Creates the appropriate {@link CompressActionFactory} for the given file name.
     *
     * @param fileName The file name.
     * @return A {@link CompressActionFactory} or {@code null} if the extension is not supported.
     */
    default @Nullable CompressActionFactory createFactoryForFileName(String fileName) {
        final int idx = fileName.lastIndexOf('.');
        if (idx != -1) {
            return createFactoryForAlgorithm(fileName.substring(idx + 1));
        }
        return null;
    }

    static CompressActionFactoryProvider newInstance(final @Nullable Configuration configuration) {
        return new CompositeCompressActionFactoryProvider(configuration);
    }
}

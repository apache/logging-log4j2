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
import java.util.zip.Deflater;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
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

    private static final class GzCompressActionFactory implements CompressActionFactory {

        static final String NAME = "gz";

        @Override
        public Action createCompressAction(Path source, Path destination, Map<String, String> options) {
            return new GzCompressAction(source, destination, Deflater.DEFAULT_COMPRESSION);
        }

        @Override
        public String getAlgorithmName() {
            return NAME;
        }
    }

    private static final class ZipCompressActionFactory implements CompressActionFactory {

        static final String NAME = "zip";

        @Override
        public Action createCompressAction(Path source, Path destination, Map<String, String> options) {
            return new ZipCompressAction(source, destination, Deflater.DEFAULT_COMPRESSION);
        }

        @Override
        public String getAlgorithmName() {
            return NAME;
        }
    }
}

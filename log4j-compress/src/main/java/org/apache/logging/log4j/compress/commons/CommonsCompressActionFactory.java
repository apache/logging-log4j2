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
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class CommonsCompressActionFactory implements CompressActionFactory {

    private final CompressorStreamProvider provider;

    private final String name;

    CommonsCompressActionFactory(final String name, final CompressorStreamProvider provider) {
        this.name = name;
        this.provider = provider;
    }

    @Override
    public Action createCompressAction(final Path source, final Path destination, final Map<String, String> ignored) {
        return new CommonsCompressAction(provider, name, source, destination);
    }

    @Override
    public String getAlgorithmName() {
        return name;
    }
}

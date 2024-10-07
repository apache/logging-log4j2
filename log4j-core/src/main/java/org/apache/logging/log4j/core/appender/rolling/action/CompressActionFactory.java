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

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NetUtils;
import org.jspecify.annotations.NullMarked;

/**
 * A factory to produce actions that compress log files.
 */
@NullMarked
public interface CompressActionFactory {

    /**
     * Returns an action that compresses a file.
     *
     * @param source The location of the file to compress.
     * @param destination The location of the compressed file.
     * @param options Compression options to pass to the compression library.
     * @return An action the compresses the source file.
     */
    Action createCompressAction(Path source, Path destination, Map<String, String> options);

    /**
     * Returns an action that compresses a file.
     *
     * @param source The location of the file to compress.
     * @param destination The location of the compressed file.
     * @param options Compression options to pass to the compression library.
     * @return An action the compresses the source file.
     */
    default Action createCompressAction(String source, String destination, Map<String, String> options) {
        return createCompressAction(toPath(source), toPath(destination), options);
    }

    private static Path toPath(final String path) {
        return FileUtils.pathFromUri(NetUtils.toURI(Objects.requireNonNull(path)));
    }

    /**
     * The name of this compression algorithm.
     * <p>
     *     When applicable, it should correspond to the name used by Apache Commons Compress.
     * </p>
     */
    String getAlgorithmName();

    /**
     * Returns the natural file extension for this compression algorithm.
     * <p>
     *     The extension must start with a dot.
     * </p>
     * @return A file extension.
     */
    default String getExtension() {
        return "." + getAlgorithmName();
    }
}

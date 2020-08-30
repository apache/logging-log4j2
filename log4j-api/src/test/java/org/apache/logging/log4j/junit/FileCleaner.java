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

import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

class FileCleaner extends AbstractFileCleaner {
    @Override
    Collection<Path> getPathsForTest(final ExtensionContext context) {
        final CleanUpFiles cleanUpFiles = context.getRequiredTestClass().getAnnotation(CleanUpFiles.class);
        return cleanUpFiles == null ? Collections.emptySet() :
                Arrays.stream(cleanUpFiles.value()).map(Paths::get).collect(Collectors.toSet());
    }

    @Override
    boolean delete(final Path path) throws IOException {
        return Files.deleteIfExists(path);
    }
}

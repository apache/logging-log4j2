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
package org.apache.logging.log4j.maven.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.instrument.Constants;
import org.apache.logging.log4j.instrument.location.LocationCache;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

public class SimpleInclusionScanner implements ClassFileInclusionScanner {

    private static final String DEFAULT_INCLUSION_PATTERN = "**/*.class";
    private static final String DEFAULT_EXCLUSION_PATTERN = "**/*" + Constants.LOCATION_CACHE_SUFFIX + ".class";

    private final long lastUpdatedWithinMsecs;
    private final Set<String> sourceIncludes;
    private final Set<String> sourceExcludes;
    private final Log log;

    public SimpleInclusionScanner(long lastUpdateWithinMsecs, Log log) {
        this(lastUpdateWithinMsecs, Set.of(DEFAULT_INCLUSION_PATTERN), Set.of(DEFAULT_EXCLUSION_PATTERN), log);
    }

    public SimpleInclusionScanner(long lastUpdateWithinMsecs, Set<String> sourceIncludes, Set<String> sourceExcludes,
            Log log) {
        this.lastUpdatedWithinMsecs = lastUpdateWithinMsecs;
        this.sourceIncludes = sourceIncludes;
        this.sourceExcludes = sourceExcludes;
        this.log = log;
    }

    @Override
    public Set<Path> getIncludedClassFiles(Path sourceDir, Path targetDir) {
        final Set<Path> potentialSources = scanForSources(sourceDir, sourceIncludes, sourceExcludes);

        return potentialSources.stream()
                .filter(source -> isLocationCacheStale(sourceDir, targetDir, source))
                .collect(Collectors.toSet());
    }

    /**
     * @return a set of relative paths to class files
     */
    private static Set<Path> scanForSources(Path sourceDir, Set<String> sourceIncludes, Set<String> sourceExcludes) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDir.toFile());
        scanner.setIncludes(sourceIncludes.toArray(String[]::new));
        scanner.setExcludes(sourceExcludes.toArray(String[]::new));
        scanner.scan();

        return Set.of(scanner.getIncludedFiles()).stream().map(Paths::get).collect(Collectors.toSet());
    }

    private boolean isLocationCacheStale(Path sourceDir, Path targetDir, Path source) {
        try {
            final Path target = targetDir.resolve(LocationCache.getCacheClassFile(source));
            if (!Files.exists(target)) {
                return true;
            }

            final FileTime sourceModifiedTime = Files.getLastModifiedTime(sourceDir.resolve(source));
            final FileTime targetModifiedTime = Files.getLastModifiedTime(target);
            return targetModifiedTime.toMillis() - sourceModifiedTime.toMillis() > lastUpdatedWithinMsecs;
        } catch (IOException e) {
            log.warn("Unable to open file: " + source, e);
        }
        return false;
    }
}

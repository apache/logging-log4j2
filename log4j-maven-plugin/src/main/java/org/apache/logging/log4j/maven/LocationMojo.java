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
package org.apache.logging.log4j.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.instrument.LocationCache;
import org.apache.logging.log4j.instrument.LocationClassConverter;
import org.apache.logging.log4j.maven.scan.ClassFileInclusionScanner;
import org.apache.logging.log4j.maven.scan.SimpleInclusionScanner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates location information for use with Log4j2.
 */
@Mojo(name = "generate-location", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class LocationMojo extends AbstractMojo {

    /**
     * The directory containing class files to process.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File sourceDirectory;

    /**
     * The directory containing processed files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    /**
     * Sets the granularity in milliseconds of the last modification date for
     * testing whether a class file needs weaving.
     */
    @Parameter(property = "lastModGranularityMs", defaultValue = "0")
    private int staleMillis;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path sourceDirectory = this.sourceDirectory.toPath();
        final Path outputDirectory = this.outputDirectory.toPath();
        final LocationCache locationCache = new LocationCache();
        final LocationClassConverter converter = new LocationClassConverter();

        try {
            final Set<Path> staleClassFiles = getClassFileInclusionScanner().getIncludedClassFiles(sourceDirectory,
                    outputDirectory);
            staleClassFiles.stream()
                    .collect(Collectors.groupingBy(LocationCache::getCacheClassFile))
                    .values()
                    .parallelStream()
                    .forEach(p -> convertClassfiles(p, converter, locationCache));

            locationCache.generateClasses().forEach(this::saveClassFile);
        } catch (WrappedIOException e) {
            throw new MojoExecutionException("An I/O error occurred.", e.getCause());
        }
    }

    private void convertClassfiles(List<Path> classFiles, LocationClassConverter converter,
            LocationCache locationCache) {
        final Path sourceDirectory = this.sourceDirectory.toPath();
        classFiles.sort(Path::compareTo);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            for (final Path classFile : classFiles) {
                buf.reset();
                try (final InputStream src = Files.newInputStream(sourceDirectory.resolve(classFile))) {
                    converter.convert(src, buf, locationCache);
                }
                saveClassFile(classFile, buf.toByteArray());
            }
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    private void saveClassFile(String internalClassName, byte[] data) {
        saveClassFile(Paths.get(internalClassName + ".class"), data);
    }

    private void saveClassFile(Path dest, byte[] data) {
        try {
            final Path outputDirectory = this.outputDirectory.toPath();
            Files.write(outputDirectory.resolve(dest), data);
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    protected ClassFileInclusionScanner getClassFileInclusionScanner() {
        return new SimpleInclusionScanner(staleMillis, getLog());
    }

    private static class WrappedIOException extends RuntimeException {

        private static final long serialVersionUID = 4290527889488735839L;

        private WrappedIOException(IOException cause) {
            super(cause);
        }

    }
}

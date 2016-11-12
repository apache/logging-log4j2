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
package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.util.BasicCommandLineArguments;

import com.beust.jcommander.Parameter;

/**
 * Tool for converting a Log4j 1.x properties configuration file to Log4j 2.x XML configuration file.
 * 
 * <p>
 * Run with "--help" on the command line.
 * </p>
 * 
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * java org.apache.log4j.config.Log4j1ConfigurationConverter --recurse
 * E:\vcs\git\apache\logging\logging-log4j2\log4j-1.2-api\src\test\resources\config-1.2\hadoop --in log4j.properties --verbose
 * </pre>
 */
public final class Log4j1ConfigurationConverter {

    public static class CommandLineArguments extends BasicCommandLineArguments {

        @Parameter(names = { "--failfast", "-f" }, description = "Fails on the first failure in recurse mode.")
        private boolean failFast;

        @Parameter(names = { "--in", "-i" }, description = "Specifies the input file.")
        private Path pathIn;

        @Parameter(names = { "--out", "-o" }, description = "Specifies the output file.")
        private Path pathOut;

        @Parameter(names = { "--recurse", "-r" }, description = "Recurses into this folder looking for the input file")
        private Path recurseIntoPath;

        @Parameter(names = { "--verbose", "-v" }, description = "Be verbose.")
        private boolean verbose;

        public Path getPathIn() {
            return pathIn;
        }

        public Path getPathOut() {
            return pathOut;
        }

        public Path getRecurseIntoPath() {
            return recurseIntoPath;
        }

        public boolean isFailFast() {
            return failFast;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setFailFast(final boolean failFast) {
            this.failFast = failFast;
        }

        public void setPathIn(final Path pathIn) {
            this.pathIn = pathIn;
        }

        public void setPathOut(final Path pathOut) {
            this.pathOut = pathOut;
        }

        public void setRecurseIntoPath(final Path recurseIntoPath) {
            this.recurseIntoPath = recurseIntoPath;
        }

        public void setVerbose(final boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return "CommandLineArguments [recurseIntoPath=" + recurseIntoPath + ", verbose=" + verbose + ", pathIn="
                    + pathIn + ", pathOut=" + pathOut + "]";
        }

    }

    private static final String FILE_EXT_XML = ".xml";

    public static void main(final String[] args) {
        new Log4j1ConfigurationConverter(BasicCommandLineArguments.parseCommandLine(args,
                Log4j1ConfigurationConverter.class, new CommandLineArguments())).run();
    }

    public static Log4j1ConfigurationConverter run(final CommandLineArguments cla) {
        final Log4j1ConfigurationConverter log4j1ConfigurationConverter = new Log4j1ConfigurationConverter(cla);
        log4j1ConfigurationConverter.run();
        return log4j1ConfigurationConverter;
    }

    private final CommandLineArguments cla;

    private Log4j1ConfigurationConverter(final CommandLineArguments cla) {
        this.cla = cla;
    }

    protected void convert(final InputStream input, final OutputStream output) throws IOException {
        final ConfigurationBuilder<BuiltConfiguration> builder = new Log4j1ConfigurationParser()
                .buildConfigurationBuilder(input);
        builder.writeXmlConfiguration(output);
    }

    InputStream getInputStream() throws IOException {
        final Path pathIn = cla.getPathIn();
        return pathIn == null ? System.in : new InputStreamWrapper(Files.newInputStream(pathIn), pathIn.toString());
    }

    OutputStream getOutputStream() throws IOException {
        final Path pathOut = cla.getPathOut();
        return pathOut == null ? System.out : Files.newOutputStream(pathOut);
    }

    private void run() {
        if (cla.getRecurseIntoPath() != null) {
            final AtomicInteger countOKs = new AtomicInteger();
            final AtomicInteger countFails = new AtomicInteger();
            try {
                Files.walkFileTree(cla.getRecurseIntoPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                            throws IOException {
                        if (cla.getPathIn() == null || file.getFileName().equals(cla.getPathIn())) {
                            verbose("Reading %s", file);
                            String newFile = file.getFileName().toString();
                            final int lastIndex = newFile.lastIndexOf(".");
                            newFile = lastIndex < 0 ? newFile + FILE_EXT_XML
                                    : newFile.substring(0, lastIndex) + FILE_EXT_XML;
                            final Path resolved = file.resolveSibling(newFile);
                            try (final InputStream input = new InputStreamWrapper(Files.newInputStream(file), file.toString());
                                    final OutputStream output = Files.newOutputStream(resolved)) {
                                try {
                                    convert(input, output);
                                    countOKs.incrementAndGet();
                                } catch (ConfigurationException | IOException e) {
                                    countFails.incrementAndGet();
                                    if (cla.isFailFast()) {
                                        throw e;
                                    }
                                    e.printStackTrace();
                                }
                                verbose("Wrote %s", resolved);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException e) {
                throw new ConfigurationException(e);
            } finally {
                verbose("OK = %,d, Failures = %,d, Total = %,d", countOKs.get(), countFails.get(),
                        countOKs.get() + countFails.get());
            }
        } else {
            verbose("Reading %s", cla.getPathIn());
            try (final InputStream input = getInputStream(); final OutputStream output = getOutputStream()) {
                convert(input, output);
            } catch (final IOException e) {
                throw new ConfigurationException(e);
            }
            verbose("Wrote %s", cla.getPathOut());
        }
    }

    private void verbose(final String template, final Object... args) {
        if (cla.isVerbose()) {
            System.err.println(String.format(template, args));
        }
    }

}

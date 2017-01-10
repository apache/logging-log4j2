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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A JUnit test rule to automatically delete files after a test is run.
 * <p>
 * For example:
 * </p>
 * 
 * <pre>
 * &#64;Rule
 * public CleanFiles files = new CleanFiles("path/to/file.txt");
 * </pre>
 *
 */
public class CleanFiles extends AbstractExternalFileCleaner {
    private static final int MAX_TRIES = 10;

    public CleanFiles(final boolean before, final boolean after, final int maxTries, final File... files) {
        super(before, after, maxTries, files);
    }

    public CleanFiles(final boolean before, final boolean after, final int maxTries, final String... fileNames) {
        super(before, after, maxTries, fileNames);
    }

    public CleanFiles(final File... files) {
        super(true, true, MAX_TRIES, files);
    }

    public CleanFiles(final Path... paths) {
        super(true, true, MAX_TRIES, paths);
    }

    public CleanFiles(final String... fileNames) {
        super(true, true, MAX_TRIES, fileNames);
    }

    @Override
    protected boolean clean(final Path path, final int tryIndex) throws IOException {
        return Files.deleteIfExists(path);
    }

}

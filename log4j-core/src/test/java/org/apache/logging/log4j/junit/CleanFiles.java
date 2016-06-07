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
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.ExternalResource;

import static org.junit.Assert.*;

/**
 * A JUnit test rule to automatically delete certain files after a test is run.
 */
public class CleanFiles extends ExternalResource {
    private static final int MAX_TRIES = 10;
    private final List<File> files;

    public CleanFiles(final File... files) {
        this.files = Arrays.asList(files);
    }

    public CleanFiles(final String... fileNames) {
        this.files = new ArrayList<>(fileNames.length);
        for (final String fileName : fileNames) {
            this.files.add(new File(fileName));
        }
    }

    private void clean() {
        for (final File file : files) {
            for (int i = 0; i < MAX_TRIES; i++) {
                if (file.exists()) {
                    try {
                        FileSystems.getDefault().provider().delete(file.toPath());
                    } catch (final IOException e) {
                        fail(e.toString());
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (final InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    protected void after() {
        this.clean();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CleanFiles [");
        builder.append(files);
        builder.append("]");
        return builder.toString();
    }
}

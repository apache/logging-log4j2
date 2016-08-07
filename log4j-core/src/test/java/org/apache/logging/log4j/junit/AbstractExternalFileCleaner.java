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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.rules.ExternalResource;

public abstract class AbstractExternalFileCleaner extends ExternalResource {

    private static final int SLEEP_RETRY_MILLIS = 200;
    private final boolean cleanAfter;
    private final boolean cleanBefore;
    private final Set<File> files;
    private final int maxTries;

    public AbstractExternalFileCleaner(final boolean before, final boolean after, final int maxTries,
            final File... files) {
        this.cleanBefore = before;
        this.cleanAfter = after;
        this.files = new HashSet<>(Arrays.asList(files));
        this.maxTries = maxTries;
    }

    public AbstractExternalFileCleaner(final boolean before, final boolean after, final int maxTries,
            final String... fileNames) {
        this.cleanBefore = before;
        this.cleanAfter = after;
        this.files = new HashSet<>(fileNames.length);
        for (final String fileName : fileNames) {
            this.files.add(new File(fileName));
        }
        this.maxTries = maxTries;
    }

    @Override
    protected void after() {
        if (cleanAfter()) {
            this.clean();
        }
    }

    @Override
    protected void before() {
        if (cleanBefore()) {
            this.clean();
        }
    }

    protected void clean() {
        Map<Path, IOException> failures = new HashMap<>();
        // Clean and gather failures
        for (final File file : getFiles()) {
            if (file.exists()) {
                final Path path = file.toPath();
                for (int i = 0; i < getMaxTries(); i++) {
                    try {
                        if (clean(path, i)) {
                            if (failures.containsKey(path)) {
                                failures.remove(path);
                            }
                            break;
                        }
                    } catch (final IOException e) {
                        // We will try again.
                        failures.put(path, e);
                    }
                    try {
                        Thread.sleep(SLEEP_RETRY_MILLIS);
                    } catch (final InterruptedException ignored) {
                        // ignore
                    }
                }
            }
        }
        // Fail on failures
        if (failures.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<Path, IOException> failure : failures.entrySet()) {
                failure.getValue().printStackTrace();
                if (!first) {
                    sb.append(", ");
                }
                sb.append(failure.getKey()).append(" failed with ").append(failure.getValue());
                first = false;
            }
            Assert.fail(sb.toString());
        }

    }

    protected abstract boolean clean(Path path, int tryIndex) throws IOException;

    public boolean cleanAfter() {
        return cleanAfter;
    }

    public boolean cleanBefore() {
        return cleanBefore;
    }

    public Set<File> getFiles() {
        return files;
    }

    public int getMaxTries() {
        return maxTries;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [files=" + files + ", cleanAfter=" + cleanAfter + ", cleanBefore="
                + cleanBefore + "]";
    }

}

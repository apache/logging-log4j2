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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.ExternalResource;

public abstract class AbstractExternalFileCleaner extends ExternalResource {

    private final boolean cleanAfter;
    private final boolean cleanBefore;
    private final List<File> files;
    private final int maxTries;

    public AbstractExternalFileCleaner(final boolean before, final boolean after, final int maxTries, final File... files) {
        this.cleanBefore = before;
        this.cleanAfter = after;
        this.files = Arrays.asList(files);
        this.maxTries = maxTries;
    }

    public AbstractExternalFileCleaner(final boolean before, final boolean after, final int maxTries, final String... fileNames) {
        this.cleanBefore = before;
        this.cleanAfter = after;
        this.files = new ArrayList<>(fileNames.length);
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

    abstract protected void clean();

    public boolean cleanAfter() {
        return cleanAfter;
    }

    public boolean cleanBefore() {
        return cleanBefore;
    }

    public List<File> getFiles() {
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

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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;

/** Base class for internal compression actions. */
public abstract class AbstractCompressAction extends AbstractAction {

    protected static final int BUF_SIZE = 8192;

    protected final File source;
    protected final File destination;
    protected final boolean deleteSource;

    protected AbstractCompressAction(final File source, final File destination, final boolean deleteSource) {
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.deleteSource = deleteSource;
    }

    @Override
    public final boolean execute() throws IOException {
        validateCompressionLevel();
        return CompressActionSupport.execute(source, destination, deleteSource, this::createCompressorOutputStream);
    }

    protected abstract OutputStream createCompressorOutputStream(OutputStream output) throws IOException;

    protected void validateCompressionLevel() {}

    public File getSource() {
        return source;
    }

    public File getDestination() {
        return destination;
    }

    public boolean isDeleteSource() {
        return deleteSource;
    }
}

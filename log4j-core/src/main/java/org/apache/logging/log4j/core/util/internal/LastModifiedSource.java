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
package org.apache.logging.log4j.core.util.internal;

import java.io.File;
import java.net.URI;
import org.apache.logging.log4j.core.util.Source;

/**
 * A Source that includes the last modified time.
 */
public class LastModifiedSource extends Source {
    private volatile long lastModified;

    public LastModifiedSource(final File file) {
        super(file);
        lastModified = 0;
    }

    public LastModifiedSource(final URI uri) {
        this(uri, 0);
    }

    public LastModifiedSource(final URI uri, final long lastModifiedMillis) {
        super(uri);
        lastModified = lastModifiedMillis;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}

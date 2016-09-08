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
package org.apache.logging.log4j.test.appender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.filter.CompositeFilter;

/**
 *
 */
public class InMemoryAppender extends AbstractOutputStreamAppender<InMemoryAppender.InMemoryManager> {

    public InMemoryAppender(final String name, final Layout<? extends Serializable> layout, final CompositeFilter filters,
                            final boolean ignoreExceptions, final boolean writeHeader) {
        super(name, layout, filters, ignoreExceptions, true, new InMemoryManager(name, layout, writeHeader));
    }

    @Override
    public String toString() {
        return getManager().toString();
    }

    static class InMemoryManager extends OutputStreamManager {

        public InMemoryManager(final String name, final Layout<? extends Serializable> layout,
                final boolean writeHeader) {
            super(new ByteArrayOutputStream(), name, layout, writeHeader);
        }

        @Override
        public String toString() {
            try {
                return getOutputStream().toString();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

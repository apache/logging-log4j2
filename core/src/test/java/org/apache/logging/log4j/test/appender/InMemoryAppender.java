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

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.filter.CompositeFilter;

import java.io.ByteArrayOutputStream;

/**
 *
 */
public class InMemoryAppender extends AbstractOutputStreamAppender {

    public InMemoryAppender(final String name, final Layout layout, final CompositeFilter filters, final boolean handleException) {
        super(name, layout, filters, handleException, true, new InMemoryManager(name));
    }

    @Override
    public String toString() {
        return getManager().toString();
    }

    private static class InMemoryManager extends OutputStreamManager {

        public InMemoryManager(final String name) {
            super(new ByteArrayOutputStream(), name);
        }

        @Override
        public String toString() {
            return getOutputStream().toString();
        }
    }
}

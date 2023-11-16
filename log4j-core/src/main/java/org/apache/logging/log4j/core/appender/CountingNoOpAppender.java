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
package org.apache.logging.log4j.core.appender;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * No-Operation Appender that counts events.
 */
@Plugin(name = "CountingNoOp", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class CountingNoOpAppender extends AbstractAppender {

    private final AtomicLong total = new AtomicLong();

    public CountingNoOpAppender(final String name, final Layout<?> layout) {
        super(name, null, layout, true, Property.EMPTY_ARRAY);
    }

    private CountingNoOpAppender(final String name, final Layout<?> layout, final Property[] properties) {
        super(name, null, layout, true, properties);
    }

    public long getCount() {
        return total.get();
    }

    @Override
    public void append(final LogEvent event) {
        total.incrementAndGet();
    }

    /**
     * Creates a CountingNoOp Appender.
     */
    @PluginFactory
    public static CountingNoOpAppender createAppender(@PluginAttribute("name") final String name) {
        return new CountingNoOpAppender(Objects.requireNonNull(name), null, Property.EMPTY_ARRAY);
    }
}

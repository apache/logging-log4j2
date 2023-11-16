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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * An Appender that ignores log events. Use for compatibility with version 1.2
 * and handy for composing a {@link ScriptAppenderSelector}.
 */
@Plugin(
        name = NullAppender.PLUGIN_NAME,
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public class NullAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "Null";

    @PluginFactory
    public static NullAppender createAppender(
            @PluginAttribute(value = "name", defaultString = "null") final String name) {
        return new NullAppender(name);
    }

    private NullAppender(final String name) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        // Do nothing
    }
}

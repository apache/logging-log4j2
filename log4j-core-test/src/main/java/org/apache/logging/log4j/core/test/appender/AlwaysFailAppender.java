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
package org.apache.logging.log4j.core.test.appender;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 *
 */
@Plugin(name = "AlwaysFail", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class AlwaysFailAppender extends AbstractAppender {

    private AlwaysFailAppender(final String name) {
        super(name, null, null, false, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
        throw new LoggingException("Always fail");
    }

    @PluginFactory
    public static AlwaysFailAppender createAppender(
            @PluginAttribute("name") @Required(message = "A name for the Appender must be specified")
                    final String name) {
        return new AlwaysFailAppender(name);
    }
}

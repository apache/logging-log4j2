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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 *
 */
@Plugin(name="FailOnce", category ="Core", elementType=Appender.ELEMENT_TYPE, printObject=true)
public class FailOnceAppender extends AbstractAppender {

    boolean fail = true;

    private final List<LogEvent> events = new ArrayList<>();

    private FailOnceAppender(final String name) {
        super(name, null, null, false);
    }

    @Override
    public void append(final LogEvent event) {
        if (fail) {
            fail = false;
            throw new LoggingException("Always fail");
        }
        events.add(event);
    }

    public List<LogEvent> getEvents() {
        final List<LogEvent> list = new ArrayList<>(events);
        events.clear();
        return list;
    }

    @PluginFactory
    public static FailOnceAppender createAppender(
        @PluginAttribute("name") @Required(message = "A name for the Appender must be specified") final String name) {
        return new FailOnceAppender(name);
    }

}

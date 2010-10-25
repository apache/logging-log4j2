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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * List could eventually grow to cause an OutOfMemoryError.
 */
 @Plugin(name="List",type="Core",elementType="appender")
public class ListAppender extends AppenderBase {

    private List<LogEvent> events = new ArrayList<LogEvent>();

    public ListAppender(String name) {
        super(name, null, null);
    }

    public ListAppender(String name, Filter[] filters) {
        super(name, filters, null);
    }

    public synchronized void append(LogEvent event) {
        events.add(event);
    }

    public synchronized void clear() {
        events.clear();
    }

    public synchronized List<LogEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @PluginFactory
    public static ListAppender createAppender(@PluginAttr("name") String name,
                                              @PluginElement("filters") Filter[] filters) {

        if (name == null) {
            logger.error("No name provided for ListAppender");
            return null;
        }

        return new ListAppender(name, filters);
    }
}

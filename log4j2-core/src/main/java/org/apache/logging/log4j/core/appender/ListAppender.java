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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
 @Plugin(name="List",type="Core")
public class ListAppender extends AppenderBase {

    private List<LogEvent> events = new ArrayList<LogEvent>();

    public ListAppender(String name) {
        super(name, null);
    }

    public synchronized void append(LogEvent event) {
        events.add(event);
    }

    public synchronized void clear() {
        events.clear();
    }

    /** @doubt think this caller would still see changes with no way 
          to synchronize so they could get an consistent snapshot.   */            
    public synchronized List<LogEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @PluginFactory
    public static ListAppender createAppender(Node node) {
        String name = null;
        for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
            if (attr.getKey().equalsIgnoreCase(NAME)) {
                name = attr.getValue();
            }
        }

        if (name == null) {
            logger.error("No name provided for Appender of type " + node.getName());
            return null;
        }

        return new ListAppender(name);
    }
}

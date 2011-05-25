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
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.Filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * List could eventually grow to cause an OutOfMemoryError.
 */
@Plugin(name = "List", type = "Core", elementType = "appender", printObject = true)
public class ListAppender extends AppenderBase {

    private List<LogEvent> events = new ArrayList<LogEvent>();

    private List<String> messages = new ArrayList<String>();

    private final boolean newLine;

    public ListAppender(String name) {
        super(name, null, null);
        newLine = false;
    }

    public ListAppender(String name, Filters filters, Layout layout, boolean newline) {
        super(name, filters, layout);
        this.newLine = newline;
        if (layout != null) {
            byte[] bytes = layout.getHeader();
            if (bytes != null) {
                write(bytes);
            }
        }
    }

    public synchronized void append(LogEvent event) {
        Layout layout = getLayout();
        if (layout == null) {
            events.add(event);
        } else {
            write(layout.format(event));
        }
    }

    private void write(byte[] bytes) {
        String str = new String(bytes);
        if (newLine) {
            int index = 0;
            while (index < str.length()) {
                int end = str.indexOf("\n", index);
                if (index == end) {
                    if (!messages.get(messages.size() - 1).equals("")) {
                        messages.add("");
                    }
                } else if (end >= 0) {
                    messages.add(str.substring(index, end));
                } else {
                    messages.add(str.substring(index));
                    break;
                }
                index = end + 1;
            }
        } else {
            messages.add(str);
        }
    }

    public void stop() {
        super.stop();
        Layout layout = getLayout();
        if (layout != null) {
            byte[] bytes = layout.getFooter();
            if (bytes != null) {
                write(bytes);
            }
        }
    }

    public synchronized void clear() {
        events.clear();
        messages.clear();
    }

    public synchronized List<LogEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    @PluginFactory
    public static ListAppender createAppender(@PluginAttr("name") String name,
                                              @PluginAttr("entryPerNewLine") String newLine,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") Filters filters) {

        if (name == null) {
            logger.error("No name provided for ListAppender");
            return null;
        }

        boolean nl = (newLine == null) ? false : Boolean.parseBoolean(newLine);

        return new ListAppender(name, filters, layout, nl);
    }
}

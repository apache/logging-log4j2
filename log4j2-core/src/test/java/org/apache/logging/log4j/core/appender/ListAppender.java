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
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.layout.SerializedLayout;

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

    private List<byte[]> data = new ArrayList<byte[]>();

    private final boolean newLine;

    private final boolean raw;

    public ListAppender(String name) {
        super(name, null, null);
        newLine = false;
        raw = false;
    }

    public ListAppender(String name, Filter filter, Layout layout, boolean newline, boolean raw) {
        super(name, filter, layout);
        this.newLine = newline;
        this.raw = raw;
        if (layout != null && !(layout instanceof SerializedLayout)) {
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
        } else if (layout instanceof SerializedLayout) {
            byte[] header = layout.getHeader();
            byte[] content = layout.format(event);
            byte[] record = new byte[header.length + content.length];
            System.arraycopy(header, 0, record, 0, header.length);
            System.arraycopy(content, 0, record, header.length, content.length);
            data.add(record);
        } else {
            write(layout.format(event));
        }
    }

    private void write(byte[] bytes) {
        if (raw) {
            data.add(bytes);
            return;
        }
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
        data.clear();
    }

    public synchronized List<LogEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public synchronized List<byte[]> getData() {
        return Collections.unmodifiableList(data);
    }

    @PluginFactory
    public static ListAppender createAppender(@PluginAttr("name") String name,
                                              @PluginAttr("entryPerNewLine") String newLine,
                                              @PluginAttr("raw") String raw,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") Filter filter) {

        if (name == null) {
            logger.error("No name provided for ListAppender");
            return null;
        }

        boolean nl = (newLine == null) ? false : Boolean.parseBoolean(newLine);
        boolean r = (raw == null) ? false : Boolean.parseBoolean(raw);

        return new ListAppender(name, filter, layout, nl, r);
    }
}

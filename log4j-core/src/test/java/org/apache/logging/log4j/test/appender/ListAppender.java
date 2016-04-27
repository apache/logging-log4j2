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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.SerializedLayout;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * List could eventually grow to cause an OutOfMemoryError.
 *
 * This appender will use {@link Layout#toByteArray(LogEvent)}.
 *
 * @see org.apache.logging.log4j.junit.LoggerContextRule#getListAppender(String) ILC.getListAppender
 */
@Plugin(name = "List", category = "Core", elementType = "appender", printObject = true)
public class ListAppender extends AbstractAppender {

    // Use CopyOnWriteArrayList?

    final List<LogEvent> events = new ArrayList<>();

    private final List<String> messages = new ArrayList<>();

    final List<byte[]> data = new ArrayList<>();

    private final boolean newLine;

    private final boolean raw;

    private static final String WINDOWS_LINE_SEP = "\r\n";

    public CountDownLatch countDownLatch = null;

    public ListAppender(final String name) {
        super(name, null, null);
        newLine = false;
        raw = false;
    }

    public ListAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean newline,
                        final boolean raw) {
        super(name, filter, layout);
        this.newLine = newline;
        this.raw = raw;
        if (layout != null && !(layout instanceof SerializedLayout)) {
            final byte[] bytes = layout.getHeader();
            if (bytes != null) {
                write(bytes);
            }
        }
    }

    @Override
    public synchronized void append(final LogEvent event) {
        final Layout<? extends Serializable> layout = getLayout();
        if (layout == null) {
            if (event instanceof MutableLogEvent) {
                // must take snapshot or subsequent calls to logger.log() will modify this event
                events.add(Log4jLogEvent.deserialize(Log4jLogEvent.serialize(event, event.isIncludeLocation())));
            } else {
                events.add(event);
            }
        } else if (layout instanceof SerializedLayout) {
            final byte[] header = layout.getHeader();
            final byte[] content = layout.toByteArray(event);
            final byte[] record = new byte[header.length + content.length];
            System.arraycopy(header, 0, record, 0, header.length);
            System.arraycopy(content, 0, record, header.length, content.length);
            data.add(record);
        } else {
            write(layout.toByteArray(event));
        }
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    void write(final byte[] bytes) {
        if (raw) {
            data.add(bytes);
            return;
        }
        final String str = new String(bytes);
        if (newLine) {
            int index = 0;
            while (index < str.length()) {
                int end;
                final int wend = str.indexOf(WINDOWS_LINE_SEP, index);
                final int lend = str.indexOf('\n', index);
                int length;
                if (wend >= 0 && wend < lend) {
                    end = wend;
                    length = 2;
                } else {
                    end = lend;
                    length = 1;
                }
                if (index == end) {
                    if (!messages.get(messages.size() - length).isEmpty()) {
                        messages.add("");
                    }
                } else if (end >= 0) {
                    messages.add(str.substring(index, end));
                } else {
                    messages.add(str.substring(index));
                    break;
                }
                index = end + length;
            }
        } else {
            messages.add(str);
        }
    }

    @Override
    public void stop() {
        super.stop();
        final Layout<? extends Serializable> layout = getLayout();
        if (layout != null) {
            final byte[] bytes = layout.getFooter();
            if (bytes != null) {
                write(bytes);
            }
        }
    }

    public synchronized ListAppender clear() {
        events.clear();
        messages.clear();
        data.clear();
        return this;
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
    public static ListAppender createAppender(
            @PluginAttribute("name") @Required(message = "No name provided for ListAppender") final String name,
            @PluginAttribute("entryPerNewLine") final boolean newLine,
            @PluginAttribute("raw") final boolean raw,
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        return new ListAppender(name, filter, layout, newLine, raw);
    }

    /**
     * Gets the named ListAppender if it has been registered.
     *
     * @param name the name of the ListAppender
     * @return the named ListAppender or {@code null} if it does not exist
     * @see org.apache.logging.log4j.junit.LoggerContextRule#getListAppender(String)
     */
    public static ListAppender getListAppender(final String name) {
        return ((ListAppender) (LoggerContext.getContext(false)).getConfiguration().getAppender(name));
    }
}

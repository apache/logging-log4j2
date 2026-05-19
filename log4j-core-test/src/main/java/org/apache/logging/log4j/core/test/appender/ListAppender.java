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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.awaitility.Awaitility;

/**
 * This appender is primarily used for testing. Use in a real environment is discouraged as the
 * lists could eventually grow to cause an {@link OutOfMemoryError}.
 *
 * <p>This appender is thread-safe: all public methods are {@code synchronized} on {@code this}.
 * Callers waiting for a minimum number of messages can use
 * {@link #getMessages(int, long, TimeUnit)} which releases the monitor while waiting.</p>
 *
 * <p>This appender will use {@link Layout#toByteArray(LogEvent)}.</p>
 *
 * @see org.apache.logging.log4j.core.test.junit.LoggerContextRule#getListAppender(String) ILC.getListAppender
 */
@Plugin(name = "List", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ListAppender extends AbstractAppender {

    final List<LogEvent> events = new ArrayList<>();

    private final List<String> messages = new ArrayList<>();

    final List<byte[]> data = new ArrayList<>();

    private final boolean newLine;

    private final boolean raw;

    private static final String WINDOWS_LINE_SEP = "\r\n";

    /**
     * A {@link CountDownLatch} that is decremented once for every call to {@link #append(LogEvent)}.
     *
     * <p>Callers may assign a new latch before submitting a known number of log events,
     * then await that latch to block until all events have been processed.  Example:</p>
     * <pre>{@code
     * listAppender.countDownLatch = new CountDownLatch(1);
     *
     * logger.info("log one event asynchronously");
     *
     * // wait for the appender to finish processing this event (wait max 1 second)
     * listAppender.countDownLatch.await(1, TimeUnit.SECONDS);
     *
     * // now assert something or do follow-up tests...
     * }</pre>
     */
    public volatile CountDownLatch countDownLatch = null;

    public ListAppender(final String name) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
        newLine = false;
        raw = false;
    }

    public ListAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean newline,
            final boolean raw) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
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
            events.add(event.toImmutable());
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
        notifyAll();
        final CountDownLatch latch = countDownLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    synchronized void write(final byte[] bytes) {
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
    public synchronized boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        final Layout<? extends Serializable> layout = getLayout();
        if (layout != null) {
            final byte[] bytes = layout.getFooter();
            if (bytes != null) {
                write(bytes);
            }
        }
        setStopped();
        return true;
    }

    public synchronized ListAppender clear() {
        events.clear();
        messages.clear();
        data.clear();
        return this;
    }

    /** Returns an immutable snapshot of captured log events */
    public synchronized List<LogEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    /** Returns an immutable snapshot of captured messages */
    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Polls the messages list for it to grow to a given minimum size at most timeout timeUnits and return a copy of
     * what we have so far.
     */
    public List<String> getMessages(final int minSize, final long timeout, final TimeUnit timeUnit)
            throws InterruptedException {
        Awaitility.waitAtMost(timeout, timeUnit).until(() -> {
            synchronized (this) {
                return messages.size() >= minSize;
            }
        });
        return getMessages();
    }

    /** Returns an immutable snapshot of captured data */
    public synchronized List<byte[]> getData() {
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    public static ListAppender createAppender(
            final String name,
            final boolean newLine,
            final boolean raw,
            final Layout<? extends Serializable> layout,
            final Filter filter) {
        return new ListAppender(name, filter, layout, newLine, raw);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ListAppender> {

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginBuilderAttribute
        private boolean entryPerNewLine;

        @PluginBuilderAttribute
        private boolean raw;

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout;

        @PluginElement("Filter")
        private Filter filter;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setEntryPerNewLine(final boolean entryPerNewLine) {
            this.entryPerNewLine = entryPerNewLine;
            return this;
        }

        public Builder setRaw(final boolean raw) {
            this.raw = raw;
            return this;
        }

        public Builder setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        public Builder setFilter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public ListAppender build() {
            return new ListAppender(name, filter, layout, entryPerNewLine, raw);
        }
    }

    /**
     * Gets the named ListAppender if it has been registered.
     *
     * @param name the name of the ListAppender
     * @return the named ListAppender or {@code null} if it does not exist
     * @see org.apache.logging.log4j.core.test.junit.LoggerContextRule#getListAppender(String)
     */
    public static ListAppender getListAppender(final String name) {
        return ((ListAppender)
                (LoggerContext.getContext(false)).getConfiguration().getAppender(name));
    }

    @Override
    public String toString() {
        return "ListAppender [events=" + events + ", messages=" + messages + ", data=" + data + ", newLine=" + newLine
                + ", raw=" + raw + ", countDownLatch=" + countDownLatch + ", getHandler()=" + getHandler()
                + ", getLayout()=" + getLayout() + ", getName()=" + getName() + ", ignoreExceptions()="
                + ignoreExceptions() + ", getFilter()=" + getFilter() + ", getState()=" + getState() + "]";
    }
}

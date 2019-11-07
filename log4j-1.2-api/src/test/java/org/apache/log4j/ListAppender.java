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
package org.apache.log4j;

import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Used to test Log4j 1 support.
 */
public class ListAppender extends AppenderSkeleton {
    // Use Collections.synchronizedList rather than CopyOnWriteArrayList because we expect
    // more frequent writes than reads.
    final List<LoggingEvent> events = Collections.synchronizedList(new ArrayList<>());

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());


    private static final String WINDOWS_LINE_SEP = "\r\n";

    @Override
    protected void append(LoggingEvent event) {
        Layout layout = getLayout();
        if (layout != null) {
            String result = layout.format(event);
            if (result != null) {
                messages.add(result);
            }
        } else {
            events.add(event);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    /** Returns an immutable snapshot of captured log events */
    public List<LoggingEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    /** Returns an immutable snapshot of captured messages */
    public List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Polls the messages list for it to grow to a given minimum size at most timeout timeUnits and return a copy of
     * what we have so far.
     */
    public List<String> getMessages(final int minSize, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        final long endMillis = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (messages.size() < minSize && System.currentTimeMillis() < endMillis) {
            Thread.sleep(100);
        }
        return getMessages();
    }
}

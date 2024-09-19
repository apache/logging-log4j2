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
package org.apache.logging.log4j.perf.appender;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.SerializedLayout;

/**
 * This appender is primarily used for testing.
 * This appender simply saves the last message logged.
 *
 * This appender will use {@link Layout#toByteArray(LogEvent)}.
 */
public class StringAppender extends AbstractAppender {
    private String message;

    public StringAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
        if (layout != null && !(layout instanceof SerializedLayout)) {
            final byte[] bytes = layout.getHeader();
            if (bytes != null) {
                message = new String(bytes);
            }
        }
    }

    @Override
    public void append(final LogEvent event) {
        final Layout<? extends Serializable> layout = getLayout();
        if (layout instanceof SerializedLayout) {
            final byte[] header = layout.getHeader();
            final byte[] content = layout.toByteArray(event);
            final byte[] record = new byte[header.length + content.length];
            System.arraycopy(header, 0, record, 0, header.length);
            System.arraycopy(content, 0, record, header.length, content.length);
            message = new String(record);
        } else {
            message = new String(layout.toByteArray(event));
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopped();
        return true;
    }

    public StringAppender clear() {
        message = null;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public static StringAppender createAppender(
            final String name, final Layout<? extends Serializable> layout, final Filter filter) {
        return new StringAppender(name, filter, layout);
    }

    /**
     * Gets the named StringAppender if it has been registered.
     *
     * @param name the name of the ListAppender
     * @return the named StringAppender or {@code null} if it does not exist
     */
    public static StringAppender getStringAppender(final String name) {
        return ((StringAppender)
                (LoggerContext.getContext(false)).getConfiguration().getAppender(name));
    }

    @Override
    public String toString() {
        return "StringAppender message=" + message;
    }
}

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
package org.apache.logging.log4j.flume.appender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.flume.event.SimpleEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.Strings;

/**
 * Class that is both a Flume and Log4j Event.
 */
public class FlumeEvent extends SimpleEvent implements LogEvent {

    static final String GUID = "guId";
    /**
     * Generated serial version ID.
     */
    private static final long serialVersionUID = -8988674608627854140L;

    private static final String DEFAULT_MDC_PREFIX = Strings.EMPTY;

    private static final String DEFAULT_EVENT_PREFIX = Strings.EMPTY;

    private static final String EVENT_TYPE = "eventType";

    private static final String EVENT_ID = "eventId";

    private static final String TIMESTAMP = "timeStamp";

    private final LogEvent event;

    private final Map<String, String> contextMap = new HashMap<>();

    private final boolean compress;

    /**
     * Construct the FlumeEvent.
     * @param event The Log4j LogEvent.
     * @param includes A comma separated list of MDC elements to include.
     * @param excludes A comma separated list of MDC elements to exclude.
     * @param required A comma separated list of MDC elements that are required to be defined.
     * @param mdcPrefix The value to prefix to MDC keys.
     * @param eventPrefix The value to prefix to event keys.
     * @param compress If true the event body should be compressed.
     */
    public FlumeEvent(final LogEvent event, final String includes, final String excludes, final String required,
                      String mdcPrefix, String eventPrefix, final boolean compress) {
        this.event = event;
        this.compress = compress;
        final Map<String, String> headers = getHeaders();
        headers.put(TIMESTAMP, Long.toString(event.getTimeMillis()));
        if (mdcPrefix == null) {
            mdcPrefix = DEFAULT_MDC_PREFIX;
        }
        if (eventPrefix == null) {
            eventPrefix = DEFAULT_EVENT_PREFIX;
        }
        final Map<String, String> mdc = event.getContextData().toMap();
        if (includes != null) {
            final String[] array = includes.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                for (String str : array) {
                    str = str.trim();
                    if (mdc.containsKey(str)) {
                        contextMap.put(str, mdc.get(str));
                    }
                }
            }
        } else if (excludes != null) {
            final String[] array = excludes.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                final List<String> list = new ArrayList<>(array.length);
                for (final String value : array) {
                    list.add(value.trim());
                }
                for (final Map.Entry<String, String> entry : mdc.entrySet()) {
                    if (!list.contains(entry.getKey())) {
                        contextMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            contextMap.putAll(mdc);
        }

        if (required != null) {
            final String[] array = required.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                for (String str : array) {
                    str = str.trim();
                    if (!mdc.containsKey(str)) {
                        throw new LoggingException("Required key " + str + " is missing from the MDC");
                    }
                }
            }
        }
        final String guid =  UuidUtil.getTimeBasedUuid().toString();
        final Message message = event.getMessage();
        if (message instanceof MapMessage) {
            // Add the guid to the Map so that it can be included in the Layout.
            ((MapMessage) message).put(GUID, guid);
            if (message instanceof StructuredDataMessage) {
                addStructuredData(eventPrefix, headers, (StructuredDataMessage) message);
            }
            addMapData(eventPrefix, headers, (MapMessage) message);
        } else {
            headers.put(GUID, guid);
        }

        addContextData(mdcPrefix, headers, contextMap);
    }

    protected void addStructuredData(final String prefix, final Map<String, String> fields,
                                     final StructuredDataMessage msg) {
        fields.put(prefix + EVENT_TYPE, msg.getType());
        final StructuredDataId id = msg.getId();
        fields.put(prefix + EVENT_ID, id.getName());
    }

    protected void addMapData(final String prefix, final Map<String, String> fields, final MapMessage msg) {
        final Map<String, String> data = msg.getData();
        for (final Map.Entry<String, String> entry : data.entrySet()) {
            fields.put(prefix + entry.getKey(), entry.getValue());
        }
    }

    protected void addContextData(final String prefix, final Map<String, String> fields,
                                  final Map<String, String> context) {
        final Map<String, String> map = new HashMap<>();
        for (final Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                fields.put(prefix + entry.getKey(), entry.getValue());
                map.put(prefix + entry.getKey(), entry.getValue());
            }
        }
        context.clear();
        context.putAll(map);
    }

	@Override
	public LogEvent toImmutable() {
		return Log4jLogEvent.createMemento(this);
	}

    /**
     * Set the body in the event.
     * @param body The body to add to the event.
     */
    @Override
    public void setBody(final byte[] body) {
        if (body == null || body.length == 0) {
            super.setBody(new byte[0]);
            return;
        }
        if (compress) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream os = new GZIPOutputStream(baos)) {
                os.write(body);
            } catch (final IOException ioe) {
                throw new LoggingException("Unable to compress message", ioe);
            }
            super.setBody(baos.toByteArray());
        } else {
            super.setBody(body);
        }
    }

    /**
     * Get the Frequently Qualified Class Name.
     * @return the FQCN String.
     */
    @Override
    public String getLoggerFqcn() {
        return event.getLoggerFqcn();
    }

    /**
     * Returns the logging Level.
     * @return the Level.
     */
    @Override
    public Level getLevel() {
        return event.getLevel();
    }

    /**
     * Returns the logger name.
     * @return the logger name.
     */
    @Override
    public String getLoggerName() {
        return event.getLoggerName();
    }

    /**
     * Returns the StackTraceElement for the caller of the logging API.
     * @return the StackTraceElement of the caller.
     */
    @Override
    public StackTraceElement getSource() {
        return event.getSource();
    }

    /**
     * Returns the Message.
     * @return the Message.
     */
    @Override
    public Message getMessage() {
        return event.getMessage();
    }

    /**
     * Returns the Marker.
     * @return the Marker.
     */
    @Override
    public Marker getMarker() {
        return event.getMarker();
    }

    /**
     * Returns the ID of the Thread.
     * @return the ID of the Thread.
     */
    @Override
    public long getThreadId() {
        return event.getThreadId();
    }

    /**
     * Returns the priority of the Thread.
     * @return the priority of the Thread.
     */
    @Override
    public int getThreadPriority() {
        return event.getThreadPriority();
    }

    /**
     * Returns the name of the Thread.
     * @return the name of the Thread.
     */
    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    /**
     * Returns the event timestamp.
     * @return the event timestamp.
     */
    @Override
    public long getTimeMillis() {
        return event.getTimeMillis();
    }

    /**
     * Returns the value of the running Java Virtual Machine's high-resolution time source when this event was created,
     * or a dummy value if it is known that this value will not be used downstream.
     * @return the event nanosecond timestamp.
     */
    @Override
    public long getNanoTime() {
        return event.getNanoTime();
    }

    /**
     * Returns the Throwable associated with the event, if any.
     * @return the Throwable.
     */
    @Override
    public Throwable getThrown() {
        return event.getThrown();
    }

    /**
     * Returns the Throwable associated with the event, if any.
     * @return the Throwable.
     */
    @Override
    public ThrowableProxy getThrownProxy() {
        return event.getThrownProxy();
    }

    /**
     * Returns a copy of the context Map.
     * @return a copy of the context Map.
     */
    @Override
    public Map<String, String> getContextMap() {
        return contextMap;
    }

    /**
     * Returns the context data of the {@code LogEvent} that this {@code FlumeEvent} was constructed with.
     * @return the context data of the {@code LogEvent} that this {@code FlumeEvent} was constructed with.
     */
    @Override
    public ReadOnlyStringMap getContextData() {
        return event.getContextData();
    }

    /**
     * Returns a copy of the context stack.
     * @return a copy of the context stack.
     */
    @Override
    public ThreadContext.ContextStack getContextStack() {
        return event.getContextStack();
    }

    @Override
    public boolean isIncludeLocation() {
        return event.isIncludeLocation();
    }

    @Override
    public void setIncludeLocation(final boolean includeLocation) {
        event.setIncludeLocation(includeLocation);
    }

    @Override
    public boolean isEndOfBatch() {
        return event.isEndOfBatch();
    }

    @Override
    public void setEndOfBatch(final boolean endOfBatch) {
        event.setEndOfBatch(endOfBatch);
    }

}

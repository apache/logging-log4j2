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

import org.apache.flume.event.SimpleEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.helpers.UUIDUtil;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Class that is both a Flume and Log4j Event.
 */
public class FlumeEvent extends SimpleEvent implements LogEvent {

    private static final String DEFAULT_MDC_PREFIX = "mdc:";

    private static final String DEFAULT_EVENT_PREFIX = "";

    private static final String EVENT_TYPE = "eventType";

    private static final String EVENT_ID = "eventId";

    private static final String GUID = "guId";

    private static final String TIMESTAMP = "timeStamp";;

    private final LogEvent event;

    private final Map<String, String> ctx = new HashMap<String, String>();

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
    public FlumeEvent(LogEvent event, String includes, String excludes, String required,
                      String mdcPrefix, String eventPrefix, boolean compress) {
        this.event = event;
        this.compress = compress;
        Map<String, String> headers = getHeaders();
        headers.put(TIMESTAMP, Long.toString(event.getMillis()));
        if (mdcPrefix == null) {
            mdcPrefix = DEFAULT_MDC_PREFIX;
        }
        if (eventPrefix == null) {
            eventPrefix = DEFAULT_EVENT_PREFIX;
        }
        Map<String, String> mdc = event.getContextMap();
        if (includes != null) {
            String[] array = includes.split(",");
            if (array.length > 0) {
                for (String str : array) {
                    if (mdc.containsKey(str)) {
                        ctx.put(str, mdc.get(str));
                    }
                }
            }
        } else if (excludes != null) {
            String[] array = excludes.split(",");
            if (array.length > 0) {
                List<String> list = Arrays.asList(array);
                for (Map.Entry<String, String> entry : mdc.entrySet()) {
                    if (!list.contains(entry.getKey())) {
                        ctx.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            ctx.putAll(mdc);
        }

        if (required != null) {
            String[] array = required.split(",");
            if (array.length > 0) {
                for (String str : array) {
                    if (!mdc.containsKey(str)) {
                        throw new LoggingException("Required key " + str + " is missing from the MDC");
                    }
                }
            }
        }
        Message message = event.getMessage();
        if (message instanceof MapMessage) {
            if (message instanceof StructuredDataMessage) {
                addStructuredData(eventPrefix, headers, (StructuredDataMessage) message);
            }
            addMapData(eventPrefix, headers, (MapMessage) message);
        }

        addContextData(mdcPrefix, headers, ctx);

        addGuid(headers);
    }

    protected void addStructuredData(String prefix, Map<String, String> fields, StructuredDataMessage msg) {
        fields.put(prefix + EVENT_TYPE, msg.getType());
        StructuredDataId id = msg.getId();
        fields.put(prefix + EVENT_ID, id.getName());
    }

    protected void addMapData(String prefix, Map<String, String> fields, MapMessage msg) {
        Map<String, String> data = msg.getData();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            fields.put(prefix + entry.getKey(), entry.getValue());
        }
    }

    protected void addContextData(String prefix, Map<String, String> fields, Map<String, String> context) {
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                fields.put(prefix + entry.getKey(), entry.getValue());
            }
        }
    }

    protected void addGuid(Map<String, String> fields) {
        fields.put(GUID, UUIDUtil.getTimeBasedUUID().toString());
    }

    /**
     * Set the body in the event.
     * @param body The body to add to the event.
     */
    @Override
    public void setBody(byte[] body) {
        if (body == null || body.length == 0) {
            super.setBody(new byte[0]);
            return;
        }
        if (compress) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                GZIPOutputStream os = new GZIPOutputStream(baos);
                os.write(body);
                os.close();
            } catch (IOException ioe) {
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
    public String getFQCN() {
        return event.getFQCN();
    }

    /**
     * Returns the logging Level.
     * @return the Level.
     */
    public Level getLevel() {
        return event.getLevel();
    }

    /**
     * Returns the logger name.
     * @return the logger name.
     */
    public String getLoggerName() {
        return event.getLoggerName();
    }

    /**
     * Returns the StackTraceElement for the caller of the logging API.
     * @return the StackTraceElement of the caller.
     */
    public StackTraceElement getSource() {
        return event.getSource();
    }

    /**
     * Returns the Message.
     * @return the Message.
     */
    public Message getMessage() {
        return event.getMessage();
    }

    /**
     * Returns the Marker.
     * @return the Marker.
     */
    public Marker getMarker() {
        return event.getMarker();
    }

    /**
     * Returns the name of the Thread.
     * @return the name of the Thread.
     */
    public String getThreadName() {
        return event.getThreadName();
    }

    /**
     * Returns the event timestamp.
     * @return the event timestamp.
     */
    public long getMillis() {
        return event.getMillis();
    }

    /**
     * Returns the Throwable associated with the event, if any.
     * @return the Throwable.
     */
    public Throwable getThrown() {
        return event.getThrown();
    }

    /**
     * Returns a copy of the context Map.
     * @return a copy of the context Map.
     */
    public Map<String, String> getContextMap() {
        return ctx;
    }

    /**
     * Returns a copy of the context stack.
     * @return a copy of the context stack.
     */
    public ThreadContext.ContextStack getContextStack() {
        return event.getContextStack();
    }
}

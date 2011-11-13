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
package org.apache.logging.log4j.core.appender.flume;

import com.cloudera.flume.core.EventBaseImpl;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
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
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class FlumeEvent extends EventBaseImpl implements LogEvent {

    private final LogEvent event;

    private byte[] body;

    private final String hostname;

    private final Map<String, String> ctx = new HashMap<String, String>();

    private static final String DEFAULT_MDC_PREFIX = "mdc:";

    private static final String DEFAULT_EVENT_PREFIX = "";

    private static final String EVENT_TYPE = "eventType";

    private static final String EVENT_ID = "eventId";

    private static final String GUID = "guId";

    private final boolean compress;

    public FlumeEvent(LogEvent event, String hostname, String includes, String excludes, String required,
                      String mdcPrefix, String eventPrefix, boolean compress) {
        this.event = event;
        this.hostname = hostname;
        this.compress = compress;
        if (mdcPrefix == null) {
            mdcPrefix = DEFAULT_MDC_PREFIX;
        }
        if (eventPrefix == null) {
            eventPrefix = DEFAULT_EVENT_PREFIX;
        }
        this.fields = new HashMap<String, byte[]>();
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
                addStructuredData(eventPrefix, fields, (StructuredDataMessage) message);
            }
            addMapData(eventPrefix, fields, (MapMessage) message);
        }

        addContextData(mdcPrefix, fields, ctx);

        addGuid(fields);
    }

    protected void addStructuredData(String prefix, Map<String, byte[]> fields, StructuredDataMessage msg) {
        fields.put(prefix + EVENT_TYPE, msg.getType().getBytes());
        StructuredDataId id = msg.getId();
        fields.put(prefix + EVENT_ID, id.getName().getBytes());
    }

    protected void addMapData(String prefix, Map<String, byte[]> fields, MapMessage msg) {
        Map<String, String> data = msg.getData();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            fields.put(prefix + entry.getKey(), entry.getValue().getBytes());
        }
    }

    protected void addContextData(String prefix, Map<String, byte[]> fields, Map<String, String> context) {
        for (Map.Entry<String, String> entry : ctx.entrySet()) {
            fields.put(prefix + entry.getKey(), entry.getValue().toString().getBytes());
        }
    }

    protected void addGuid(Map<String, byte[]> fields) {
        fields.put(GUID, UUIDUtil.getTimeBasedUUID().toString().getBytes());
    }

    public void setBody(byte[] body) {
        if (body == null || body.length == 0) {
            this.body = new byte[0];
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
            this.body = baos.toByteArray();
        } else {
            this.body = body;
        }
    }

    @Override
    public byte[] getBody() {
        return this.body;
    }

    @Override
    public Priority getPriority() {
        switch (event.getLevel()) {
            case INFO:
                return Priority.INFO;
            case ERROR:
                return Priority.ERROR;
            case DEBUG:
                return Priority.DEBUG;
            case WARN:
                return Priority.WARN;
            case TRACE:
                return Priority.TRACE;
            case FATAL:
                return Priority.FATAL;
        }
        return Priority.INFO;
    }

    public String getFQCN() {
        return event.getFQCN();
    }

    @Override
    public long getTimestamp() {
        return event.getMillis();
    }

    @Override
    public long getNanos() {
        return System.nanoTime();
    }

    @Override
    public String getHost() {
        return hostname;
    }

    public Level getLevel() {
        return event.getLevel();
    }

    public String getLoggerName() {
        return event.getLoggerName();
    }

    public StackTraceElement getSource() {
        return event.getSource();
    }

    public Message getMessage() {
        return event.getMessage();
    }

    public Marker getMarker() {
        return event.getMarker();
    }

    public String getThreadName() {
        return event.getThreadName();
    }

    public long getMillis() {
        return event.getMillis();
    }

    public Throwable getThrown() {
        return event.getThrown();
    }

    public Map<String, String> getContextMap() {
        return ctx;
    }

    public Stack<String> getContextStack() {
        return event.getContextStack();
    }
}

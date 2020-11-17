/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.log4j.rewrite;

import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;

import java.util.HashMap;
import java.util.Map;

/**
 * This policy rewrites events where the message of the
 * original event implements java.util.Map.
 * All other events are passed through unmodified.
 * If the map contains a "message" entry, the value will be
 * used as the message for the rewritten event.  The rewritten
 * event will have a property set that is the combination of the
 * original property set and the other members of the message map.
 * If both the original property set and the message map
 * contain the same entry, the value from the message map
 * will overwrite the original property set.
 * <p>
 * The combination of the RewriteAppender and this policy
 * performs the same actions as the MapFilter from log4j 1.3.
 * </p>
 */
public class MapRewritePolicy implements RewritePolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    public LoggingEvent rewrite(final LoggingEvent source) {
        Object msg = source.getMessage();
        if (msg instanceof MapMessage || msg instanceof Map) {
            Map<String, String> props = source.getProperties() != null ? new HashMap<>(source.getProperties())
                    : new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> eventProps = msg instanceof Map ? (Map) msg : ((MapMessage) msg).getData();
            //
            //   if the map sent in the logging request
            //      has "message" entry, use that as the message body
            //      otherwise, use the entire map.
            //
            Message newMessage = null;
            Object newMsg = eventProps.get("message");
            if (newMsg != null) {
                newMessage = new SimpleMessage(newMsg.toString());
                for (Map.Entry<String, Object> entry : eventProps.entrySet()) {
                    if (!("message".equals(entry.getKey()))) {
                        props.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            } else {
                return source;
            }

            LogEvent event;
            if (source instanceof LogEventAdapter) {
                event = new Log4jLogEvent.Builder(((LogEventAdapter) source).getEvent())
                        .setMessage(newMessage)
                        .setContextData(new SortedArrayStringMap(props))
                        .build();
            } else {
                LocationInfo info = source.getLocationInformation();
                StackTraceElement element = new StackTraceElement(info.getClassName(), info.getMethodName(),
                        info.getFileName(), Integer.parseInt(info.getLineNumber()));
                Thread thread = getThread(source.getThreadName());
                long threadId = thread != null ? thread.getId() : 0;
                int threadPriority = thread != null ? thread.getPriority() : 0;
                event = Log4jLogEvent.newBuilder()
                        .setContextData(new SortedArrayStringMap(props))
                        .setLevel(OptionConverter.convertLevel(source.getLevel()))
                        .setLoggerFqcn(source.getFQNOfLoggerClass())
                        .setMarker(null)
                        .setMessage(newMessage)
                        .setSource(element)
                        .setLoggerName(source.getLoggerName())
                        .setThreadName(source.getThreadName())
                        .setThreadId(threadId)
                        .setThreadPriority(threadPriority)
                        .setThrown(source.getThrowableInformation().getThrowable())
                        .setTimeMillis(source.getTimeStamp())
                        .setNanoTime(0)
                        .setThrownProxy(null)
                        .build();
            }
            return new LogEventAdapter(event);
        }
        return source;

    }

    private Thread getThread(String name) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().equals(name)) {
                return thread;
            }
        }
        return null;
    }
}

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
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This policy rewrites events by adding
 * a user-specified list of properties to the event.
 * Existing properties are not modified.
 * <p>
 * The combination of the RewriteAppender and this policy
 * performs the same actions as the PropertyFilter from log4j 1.3.
 * </p>
 */
public class PropertyRewritePolicy implements RewritePolicy {
    private Map<String, String> properties = Collections.EMPTY_MAP;

    public PropertyRewritePolicy() {
    }

    /**
     * Set a string representing the property name/value pairs.
     * <p>
     * Form:
     * </p>
     * <pre>
     * propname1=propvalue1,propname2=propvalue2
     * </pre>
     *
     * @param properties The properties.
     */
    public void setProperties(String properties) {
        Map<String, String> newMap = new HashMap<>();
        StringTokenizer pairs = new StringTokenizer(properties, ",");
        while (pairs.hasMoreTokens()) {
            StringTokenizer entry = new StringTokenizer(pairs.nextToken(), "=");
            newMap.put(entry.nextElement().toString().trim(), entry.nextElement().toString().trim());
        }
        synchronized (this) {
            this.properties = newMap;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoggingEvent rewrite(final LoggingEvent source) {
        if (!properties.isEmpty()) {
            Map<String, String> rewriteProps = source.getProperties() != null ? new HashMap<>(source.getProperties())
                    : new HashMap<>();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (!rewriteProps.containsKey(entry.getKey())) {
                    rewriteProps.put(entry.getKey(), entry.getValue());
                }
            }
            LogEvent event;
            if (source instanceof LogEventAdapter) {
                event = new Log4jLogEvent.Builder(((LogEventAdapter) source).getEvent())
                        .setContextData(new SortedArrayStringMap(rewriteProps))
                        .build();
            } else {
                LocationInfo info = source.getLocationInformation();
                StackTraceElement element = new StackTraceElement(info.getClassName(), info.getMethodName(),
                        info.getFileName(), Integer.parseInt(info.getLineNumber()));
                Thread thread = getThread(source.getThreadName());
                long threadId = thread != null ? thread.getId() : 0;
                int threadPriority = thread != null ? thread.getPriority() : 0;
                event = Log4jLogEvent.newBuilder()
                        .setContextData(new SortedArrayStringMap(rewriteProps))
                        .setLevel(OptionConverter.convertLevel(source.getLevel()))
                        .setLoggerFqcn(source.getFQNOfLoggerClass())
                        .setMarker(null)
                        .setMessage(new SimpleMessage(source.getRenderedMessage()))
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

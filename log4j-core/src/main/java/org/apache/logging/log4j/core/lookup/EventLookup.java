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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Looks up values from the log event.
 */
@Plugin(name = "event", category = StrLookup.CATEGORY)
public class EventLookup extends AbstractLookup {

    /**
     * Looks up the value from the logging event.
     * @param event The current LogEvent.
     * @param key  the key to be looked up.
     * @return The value of the specified log event field.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        if (event == null) {
            return null;
        }
        switch (key) {
            case "Marker": {
                return event.getMarker() != null ? event.getMarker().getName() : null;
            }
            case "ThreadName": {
                return event.getThreadName();
            }
            case "Level": {
                return event.getLevel().toString();
            }
            case "ThreadId": {
                return Long.toString(event.getThreadId());
            }
            case "Timestamp": {
                return Long.toString(event.getTimeMillis());
            }
            case "Exception": {
                if (event.getThrown() != null) {
                    return event.getThrown().getClass().getSimpleName();
                }
                if (event.getThrownProxy() != null) {
                    return event.getThrownProxy().getName();
                }
                return null;
            }
            case "Logger": {
                return event.getLoggerName();
            }
            case "Message": {
                return event.getMessage().getFormattedMessage();
            }
            default: {
                return null;
            }
        }
    }
}

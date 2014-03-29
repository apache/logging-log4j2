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
package org.apache.logging.slf4j;

import java.util.Map;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.slf4j.ext.EventData;

/**
 *
 */
public class EventDataConverter {

    public Message convertEvent(final String message, final Object[] objects, final Throwable throwable) {
        try {
            final EventData data = objects != null && objects[0] instanceof EventData ?
                    (EventData) objects[0] : new EventData(message);
            final StructuredDataMessage msg =
                    new StructuredDataMessage(data.getEventId(), data.getMessage(), data.getEventType());
            for (final Map.Entry<String, Object> entry : data.getEventMap().entrySet()) {
                final String key = entry.getKey();
                if (EventData.EVENT_TYPE.equals(key) || EventData.EVENT_ID.equals(key)
                        || EventData.EVENT_MESSAGE.equals(key)) {
                    continue;
                }
                msg.put(key, String.valueOf(entry.getValue()));
            }
            return msg;
        } catch (final Exception ex) {
            return new ParameterizedMessage(message, objects, throwable);
        }
    }
}

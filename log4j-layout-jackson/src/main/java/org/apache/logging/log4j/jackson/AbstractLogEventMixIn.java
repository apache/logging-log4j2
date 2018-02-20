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
package org.apache.logging.log4j.jackson;

import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * <pre>
 * AbstractLogEventMixIn
*├─ AbstractLogEventXmlMixIn
*├──── LogEventWithContextListXmlMixIn
*├──── LogEventWithContextMapXmlMixIn
*├─ AbstractLogEventJsonMixIn
*├──── LogEventWithContextListJsonMixIn
*├──── LogEventWithContextMapJsonMixIn
 * </pre>
 */
@JsonFilter(AbstractLogEventMixIn.JSON_FILTER_ID)
public abstract class AbstractLogEventMixIn implements LogEvent {

    public static final String ATTR_END_OF_BATCH = "endOfBatch";
    public static final String ATTR_LEVEL = "level";
    public static final String ATTR_LOGGER_FQCN = "loggerFqcn";
    public static final String ATTR_LOGGER_NAME = "loggerName";
    public static final String ATTR_MARKER = "marker";
    public static final String ATTR_THREAD = "thread";
    public static final String ATTR_THREAD_ID = "threadId";
    public static final String ATTR_THREAD_PRIORITY = "threadPriority";
    public static final String ELT_MESSAGE = "message";
    public static final String JSON_FILTER_ID = "org.apache.logging.log4j.core.impl.Log4jLogEvent";

    private static final long serialVersionUID = 1L;

    @Deprecated
    @Override
    @JsonIgnore
    public abstract Map<String, String> getContextMap();

    @JsonSerialize(using = MessageSerializer.class)
    @JsonDeserialize(using = SimpleMessageDeserializer.class)
    @Override
    public abstract Message getMessage();

    @JsonIgnore
    @Override
    public abstract Throwable getThrown();

    @JsonIgnore // ignore from 2.11.0
    @Override
    public abstract long getTimeMillis();

    @JsonIgnore
    @Override
    public abstract boolean isIncludeLocation();

    @Override
    public abstract void setEndOfBatch(boolean endOfBatch);

    @Override
    public abstract void setIncludeLocation(boolean locationRequired);

}

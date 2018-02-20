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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonRootName(XmlConstants.ELT_EVENT)
@JsonFilter("org.apache.logging.log4j.core.impl.Log4jLogEvent")
@JsonPropertyOrder({ "timeMillis", XmlConstants.ELT_INSTANT, "threadName", "level", "loggerName", "marker", "message", "thrown", XmlConstants.ELT_CONTEXT_MAP,
        JsonConstants.ELT_CONTEXT_STACK, "loggerFQCN", "Source", "endOfBatch" })
abstract class LogEventWithContextListMixIn implements LogEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(JsonConstants.ELT_CONTEXT_MAP)
    @JsonSerialize(using = ContextDataAsEntryListSerializer.class)
    @JsonDeserialize(using = ContextDataAsEntryListDeserializer.class)
//    @JsonIgnore
    @Override
    public abstract ReadOnlyStringMap getContextData();

    @Override
    @JsonIgnore
    public abstract Map<String, String> getContextMap();

    @JsonProperty(JsonConstants.ELT_CONTEXT_STACK)
    @Override
    public abstract ContextStack getContextStack();

    @JsonProperty(JsonConstants.ELT_INSTANT)
    @Override
    public abstract Instant getInstant();

    @JsonProperty()
    @Override
    public abstract Level getLevel();

    @JsonProperty()
    @Override
    public abstract String getLoggerFqcn();

    @JsonProperty()
    @Override
    public abstract String getLoggerName();

    @JsonProperty(JsonConstants.ELT_MARKER)
    @Override
    public abstract Marker getMarker();

    @JsonProperty(JsonConstants.ELT_MESSAGE)
    @JsonSerialize(using = MessageSerializer.class)
    @JsonDeserialize(using = SimpleMessageDeserializer.class)
    @Override
    public abstract Message getMessage();

    @JsonProperty(JsonConstants.ELT_SOURCE)
    @JsonDeserialize(using = Log4jStackTraceElementDeserializer.class)
    @Override
    public abstract StackTraceElement getSource();

    @Override
    @JsonProperty("threadId")
    public abstract long getThreadId();

    @Override
    @JsonProperty("thread")
    public abstract String getThreadName();

    @Override
    @JsonProperty("threadPriority")
    public abstract int getThreadPriority();

    @JsonIgnore
    @Override
    public abstract Throwable getThrown();

    @JsonProperty(JsonConstants.ELT_THROWN)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JsonIgnore // ignore from 2.11
    @Override
    public abstract long getTimeMillis();

    @JsonProperty()
    @Override
    public abstract boolean isEndOfBatch();

    @JsonIgnore
    @Override
    public abstract boolean isIncludeLocation();

    @Override
    public abstract void setEndOfBatch(boolean endOfBatch);

    @Override
    public abstract void setIncludeLocation(boolean locationRequired);

}

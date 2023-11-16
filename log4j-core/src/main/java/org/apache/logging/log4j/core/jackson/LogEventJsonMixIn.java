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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

@JsonRootName(XmlConstants.ELT_EVENT)
@JacksonXmlRootElement(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_EVENT)
@JsonFilter("org.apache.logging.log4j.core.impl.Log4jLogEvent")
@JsonPropertyOrder({
    "timeMillis",
    JsonConstants.ELT_INSTANT,
    "threadName",
    "level",
    "loggerName",
    "marker",
    "message",
    "thrown",
    XmlConstants.ELT_CONTEXT_MAP,
    JsonConstants.ELT_CONTEXT_STACK,
    "loggerFQCN",
    "Source",
    "endOfBatch"
})
abstract class LogEventJsonMixIn implements LogEvent {

    private static final long serialVersionUID = 1L;

    //    @JsonProperty(JsonConstants.ELT_CONTEXT_MAP)
    //    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_MAP)
    //    @JsonSerialize(using = MapSerializer.class)
    //    @JsonDeserialize(using = MapDeserializer.class)
    @Override
    @JsonIgnore
    //    @JsonProperty(JsonConstants.ELT_CONTEXT_MAP)
    //    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_MAP)
    public abstract Map<String, String> getContextMap();

    @JsonProperty(JsonConstants.ELT_CONTEXT_MAP)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_MAP)
    @JsonSerialize(using = ContextDataSerializer.class)
    @JsonDeserialize(using = ContextDataDeserializer.class)
    // @JsonIgnore
    @Override
    public abstract ReadOnlyStringMap getContextData();

    @JsonProperty(JsonConstants.ELT_CONTEXT_STACK)
    @JacksonXmlElementWrapper(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_STACK)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_STACK_ITEM)
    @Override
    public abstract ContextStack getContextStack();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract Level getLevel();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerFqcn();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerName();

    @JsonProperty(JsonConstants.ELT_MARKER)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_MARKER)
    @Override
    public abstract Marker getMarker();

    @JsonProperty(JsonConstants.ELT_MESSAGE)
    @JsonDeserialize(using = SimpleMessageDeserializer.class)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_MESSAGE)
    @Override
    public abstract Message getMessage();

    @JsonProperty(JsonConstants.ELT_SOURCE)
    @JsonDeserialize(using = Log4jStackTraceElementDeserializer.class)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SOURCE)
    @Override
    public abstract StackTraceElement getSource();

    @Override
    @JsonProperty("threadId")
    @JacksonXmlProperty(isAttribute = true, localName = "threadId")
    public abstract long getThreadId();

    @Override
    @JsonProperty("thread")
    @JacksonXmlProperty(isAttribute = true, localName = "thread")
    public abstract String getThreadName();

    @Override
    @JsonProperty("threadPriority")
    @JacksonXmlProperty(isAttribute = true, localName = "threadPriority")
    public abstract int getThreadPriority();

    @JsonIgnore
    @Override
    public abstract Throwable getThrown();

    @JsonProperty(JsonConstants.ELT_THROWN)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_THROWN)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JsonProperty(value = JsonConstants.ELT_TIME_MILLIS, access = JsonProperty.Access.READ_ONLY)
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract long getTimeMillis();

    @JsonProperty(JsonConstants.ELT_INSTANT)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_INSTANT)
    @Override
    public abstract Instant getInstant();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
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

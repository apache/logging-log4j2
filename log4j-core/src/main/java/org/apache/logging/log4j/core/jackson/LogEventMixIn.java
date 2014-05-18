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
package org.apache.logging.log4j.core.jackson;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;

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

@JsonRootName(XMLConstants.ELT_EVENT)
@JacksonXmlRootElement(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_EVENT)
@JsonFilter("org.apache.logging.log4j.core.impl.Log4jLogEvent")
@JsonPropertyOrder({ "timeMillis", "threadName", "level", "loggerName", "marker", "message", "thrown", XMLConstants.ELT_CONTEXT_MAP,
        JSONConstants.ELT_CONTEXT_STACK, "loggerFQCN", "Source", "endOfBatch" })
abstract class LogEventMixIn implements LogEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(JSONConstants.ELT_CONTEXT_MAP)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_CONTEXT_MAP)
    @JsonSerialize(using = ListOfMapEntrySerializer.class)
    @JsonDeserialize(using = ListOfMapEntryDeserializer.class)
    @Override
    public abstract Map<String, String> getContextMap();

    @JsonProperty(JSONConstants.ELT_CONTEXT_STACK)
    @JacksonXmlElementWrapper(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_CONTEXT_STACK)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_CONTEXT_STACK_ITEM)
    @Override
    public abstract ContextStack getContextStack();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract Level getLevel();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerFQCN();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerName();

    @JsonProperty(JSONConstants.ELT_MARKER)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_MARKER)
    @Override
    public abstract Marker getMarker();

    @JsonProperty(JSONConstants.ELT_MESSAGE)
    @JsonSerialize(using = MessageSerializer.class)
    @JsonDeserialize(using = SimpleMessageDeserializer.class)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_MESSAGE)
    @Override
    public abstract Message getMessage();

    @JsonProperty(JSONConstants.ELT_SOURCE)
    @JsonDeserialize(using = Log4jStackTraceElementDeserializer.class)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_SOURCE)
    @Override
    public abstract StackTraceElement getSource();

    @Override
    @JsonProperty("thread")
    @JacksonXmlProperty(isAttribute = true, localName = "thread")
    public abstract String getThreadName();

    @JsonIgnore
    @Override
    public abstract Throwable getThrown();

    @JsonProperty(JSONConstants.ELT_THROWN)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_THROWN)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JsonProperty()
    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract long getTimeMillis();

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

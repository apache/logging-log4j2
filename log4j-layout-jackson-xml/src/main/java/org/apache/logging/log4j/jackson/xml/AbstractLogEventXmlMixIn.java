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
package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.jackson.AbstractLogEventMixIn;
import org.apache.logging.log4j.jackson.ContextDataAsEntryListDeserializer;
import org.apache.logging.log4j.jackson.Log4jStackTraceElementDeserializer;
import org.apache.logging.log4j.jackson.MessageSerializer;
import org.apache.logging.log4j.jackson.SimpleMessageDeserializer;
import org.apache.logging.log4j.jackson.XmlConstants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
* <pre>AbstractLogEventMixIn
*├─ XmlLogEventMixIn
*├──── XmlLogEventWithContextListMixIn
*├──── XmlLogEventWithContextMapMixIn
*├─ JsonLogEventMixIn
*├──── JsonLogEventWithContextListMixIn
*├──── JsonLogEventWithContextMapMixIn</pre>
*/
@JacksonXmlRootElement(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_EVENT)
@JsonFilter(AbstractLogEventMixIn.JSON_FILTER_ID)
@JsonPropertyOrder({
    // @formatter:off
    AbstractLogEventMixIn.ATTR_THREAD,
    AbstractLogEventMixIn.ATTR_LEVEL,
    AbstractLogEventMixIn.ATTR_LOGGER_NAME,
    AbstractLogEventMixIn.ATTR_LOGGER_FQCN,
    AbstractLogEventMixIn.ATTR_END_OF_BATCH,
    XmlConstants.ELT_INSTANT,
    XmlConstants.ELT_MARKER,
    XmlConstants.ELT_MESSAGE,
    AbstractLogEventXmlMixIn.ELT_THROWN,
    XmlConstants.ELT_CONTEXT_MAP,
    XmlConstants.ELT_CONTEXT_STACK,
    XmlConstants.ELT_SOURCE})
    // @formatter:on
public abstract class AbstractLogEventXmlMixIn extends AbstractLogEventMixIn {

    public static final String ELT_THROWN = "Thrown";

    private static final long serialVersionUID = 1L;

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_MAP)
    @JsonSerialize(using = ContextDataAsEntryListXmlSerializer.class)
    @JsonDeserialize(using = ContextDataAsEntryListDeserializer.class)
    @Override
    public abstract ReadOnlyStringMap getContextData();

    @JacksonXmlElementWrapper(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_STACK)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_STACK_ITEM)
    @Override
    public abstract ContextStack getContextStack();

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_INSTANT)
    @Override
    public abstract Instant getInstant();

    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract Level getLevel();

    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerFqcn();

    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract String getLoggerName();

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_MARKER)
    @Override
    public abstract Marker getMarker();

    @JsonSerialize(using = MessageSerializer.class)
    @JsonDeserialize(using = SimpleMessageDeserializer.class)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_MESSAGE)
    @Override
    public abstract Message getMessage();

    @JsonDeserialize(using = Log4jStackTraceElementDeserializer.class)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SOURCE)
    @Override
    public abstract StackTraceElement getSource();

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = ATTR_THREAD_ID)
    public abstract long getThreadId();

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = ATTR_THREAD)
    public abstract String getThreadName();

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = ATTR_THREAD_PRIORITY)
    public abstract int getThreadPriority();

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_THROWN)
    @Override
    public abstract ThrowableProxy getThrownProxy();

    @JacksonXmlProperty(isAttribute = true)
    @Override
    public abstract boolean isEndOfBatch();

}

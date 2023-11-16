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
package org.apache.logging.log4j.core.layout;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.Strings;

abstract class AbstractJacksonLayout extends AbstractStringLayout {

    protected static final String DEFAULT_EOL = "\r\n";
    protected static final String COMPACT_EOL = Strings.EMPTY;

    public abstract static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B> {

        @PluginBuilderAttribute
        private boolean eventEol;

        @PluginBuilderAttribute
        private String endOfLine;

        @PluginBuilderAttribute
        private boolean compact;

        @PluginBuilderAttribute
        private boolean complete;

        @PluginBuilderAttribute
        private boolean locationInfo;

        @PluginBuilderAttribute
        private boolean properties;

        @PluginBuilderAttribute
        private boolean includeStacktrace = true;

        @PluginBuilderAttribute
        private boolean stacktraceAsString = false;

        @PluginBuilderAttribute
        private boolean includeNullDelimiter = false;

        @PluginBuilderAttribute
        private boolean includeTimeMillis = false;

        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        protected String toStringOrNull(final byte[] header) {
            return header == null ? null : new String(header, Charset.defaultCharset());
        }

        public boolean getEventEol() {
            return eventEol;
        }

        public String getEndOfLine() {
            return endOfLine;
        }

        public boolean isCompact() {
            return compact;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean isLocationInfo() {
            return locationInfo;
        }

        public boolean isProperties() {
            return properties;
        }

        /**
         * If "true", includes the stacktrace of any Throwable in the generated data, defaults to "true".
         * @return If "true", includes the stacktrace of any Throwable in the generated data, defaults to "true".
         */
        public boolean isIncludeStacktrace() {
            return includeStacktrace;
        }

        public boolean isStacktraceAsString() {
            return stacktraceAsString;
        }

        public boolean isIncludeNullDelimiter() {
            return includeNullDelimiter;
        }

        public boolean isIncludeTimeMillis() {
            return includeTimeMillis;
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        public B setEventEol(final boolean eventEol) {
            this.eventEol = eventEol;
            return asBuilder();
        }

        public B setEndOfLine(final String endOfLine) {
            this.endOfLine = endOfLine;
            return asBuilder();
        }

        public B setCompact(final boolean compact) {
            this.compact = compact;
            return asBuilder();
        }

        public B setComplete(final boolean complete) {
            this.complete = complete;
            return asBuilder();
        }

        public B setLocationInfo(final boolean locationInfo) {
            this.locationInfo = locationInfo;
            return asBuilder();
        }

        public B setProperties(final boolean properties) {
            this.properties = properties;
            return asBuilder();
        }

        /**
         * If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         * @param includeStacktrace If "true", includes the stacktrace of any Throwable in the generated JSON, defaults to "true".
         * @return this builder
         */
        public B setIncludeStacktrace(final boolean includeStacktrace) {
            this.includeStacktrace = includeStacktrace;
            return asBuilder();
        }

        /**
         * Whether to format the stacktrace as a string, and not a nested object (optional, defaults to false).
         *
         * @return this builder
         */
        public B setStacktraceAsString(final boolean stacktraceAsString) {
            this.stacktraceAsString = stacktraceAsString;
            return asBuilder();
        }

        /**
         * Whether to include NULL byte as delimiter after each event (optional, default to false).
         *
         * @return this builder
         */
        public B setIncludeNullDelimiter(final boolean includeNullDelimiter) {
            this.includeNullDelimiter = includeNullDelimiter;
            return asBuilder();
        }

        /**
         * Whether to include the timestamp (in addition to the Instant) (optional, default to false).
         *
         * @return this builder
         */
        public B setIncludeTimeMillis(final boolean includeTimeMillis) {
            this.includeTimeMillis = includeTimeMillis;
            return asBuilder();
        }

        /**
         * Additional fields to set on each log event.
         *
         * @return this builder
         */
        public B setAdditionalFields(final KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return asBuilder();
        }
    }

    protected final String eol;
    protected final ObjectWriter objectWriter;
    protected final boolean compact;
    protected final boolean complete;
    protected final boolean includeNullDelimiter;
    protected final ResolvableKeyValuePair[] additionalFields;

    @Deprecated
    protected AbstractJacksonLayout(
            final Configuration config,
            final ObjectWriter objectWriter,
            final Charset charset,
            final boolean compact,
            final boolean complete,
            final boolean eventEol,
            final Serializer headerSerializer,
            final Serializer footerSerializer) {
        this(config, objectWriter, charset, compact, complete, eventEol, headerSerializer, footerSerializer, false);
    }

    @Deprecated
    protected AbstractJacksonLayout(
            final Configuration config,
            final ObjectWriter objectWriter,
            final Charset charset,
            final boolean compact,
            final boolean complete,
            final boolean eventEol,
            final Serializer headerSerializer,
            final Serializer footerSerializer,
            final boolean includeNullDelimiter) {
        this(
                config,
                objectWriter,
                charset,
                compact,
                complete,
                eventEol,
                null,
                headerSerializer,
                footerSerializer,
                includeNullDelimiter,
                null);
    }

    protected AbstractJacksonLayout(
            final Configuration config,
            final ObjectWriter objectWriter,
            final Charset charset,
            final boolean compact,
            final boolean complete,
            final boolean eventEol,
            final String endOfLine,
            final Serializer headerSerializer,
            final Serializer footerSerializer,
            final boolean includeNullDelimiter,
            final KeyValuePair[] additionalFields) {
        super(config, charset, headerSerializer, footerSerializer);
        this.objectWriter = objectWriter;
        this.compact = compact;
        this.complete = complete;
        this.eol = endOfLine != null ? endOfLine : compact && !eventEol ? COMPACT_EOL : DEFAULT_EOL;
        this.includeNullDelimiter = includeNullDelimiter;
        this.additionalFields = prepareAdditionalFields(config, additionalFields);
    }

    protected static boolean valueNeedsLookup(final String value) {
        return value != null && value.contains("${");
    }

    private static ResolvableKeyValuePair[] prepareAdditionalFields(
            final Configuration config, final KeyValuePair[] additionalFields) {
        if (additionalFields == null || additionalFields.length == 0) {
            // No fields set
            return ResolvableKeyValuePair.EMPTY_ARRAY;
        }

        // Convert to specific class which already determines whether values needs lookup during serialization
        final ResolvableKeyValuePair[] resolvableFields = new ResolvableKeyValuePair[additionalFields.length];

        for (int i = 0; i < additionalFields.length; i++) {
            final ResolvableKeyValuePair resolvable =
                    resolvableFields[i] = new ResolvableKeyValuePair(additionalFields[i]);

            // Validate
            if (config == null && resolvable.valueNeedsLookup) {
                throw new IllegalArgumentException(
                        "configuration needs to be set when there are additional fields with variables");
            }
        }

        return resolvableFields;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent}.
     *
     * @param event The LogEvent.
     * @return The XML representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilderWriter writer = new StringBuilderWriter();
        try {
            toSerializable(event, writer);
            return writer.toString();
        } catch (final IOException e) {
            // Should this be an ISE or IAE?
            LOGGER.error(e);
            return Strings.EMPTY;
        }
    }

    private static LogEvent convertMutableToLog4jEvent(final LogEvent event) {
        // TODO Jackson-based layouts have certain filters set up for Log4jLogEvent.
        // TODO Need to set up the same filters for MutableLogEvent but don't know how...
        // This is a workaround.
        return event instanceof Log4jLogEvent ? event : Log4jLogEvent.createMemento(event);
    }

    protected Object wrapLogEvent(final LogEvent event) {
        if (additionalFields.length > 0) {
            // Construct map for serialization - note that we are intentionally using original LogEvent
            final Map<String, String> additionalFieldsMap = resolveAdditionalFields(event);
            // This class combines LogEvent with AdditionalFields during serialization
            return new LogEventWithAdditionalFields(event, additionalFieldsMap);
        } else if (event instanceof Message) {
            // If the LogEvent implements the Messagee interface Jackson will not treat is as a LogEvent.
            return new ReadOnlyLogEventWrapper(event);
        } else {
            // No additional fields, return original object
            return event;
        }
    }

    private Map<String, String> resolveAdditionalFields(final LogEvent logEvent) {
        // Note: LinkedHashMap retains order
        final Map<String, String> additionalFieldsMap = new LinkedHashMap<>(additionalFields.length);
        final StrSubstitutor strSubstitutor = configuration.getStrSubstitutor();

        // Go over each field
        for (final ResolvableKeyValuePair pair : additionalFields) {
            if (pair.valueNeedsLookup) {
                // Resolve value
                additionalFieldsMap.put(pair.key, strSubstitutor.replace(logEvent, pair.value));
            } else {
                // Plain text value
                additionalFieldsMap.put(pair.key, pair.value);
            }
        }

        return additionalFieldsMap;
    }

    public void toSerializable(final LogEvent event, final Writer writer)
            throws JsonGenerationException, JsonMappingException, IOException {
        objectWriter.writeValue(writer, wrapLogEvent(convertMutableToLog4jEvent(event)));
        writer.write(eol);
        if (includeNullDelimiter) {
            writer.write('\0');
        }
        markEvent();
    }

    @JsonRootName(XmlConstants.ELT_EVENT)
    @JacksonXmlRootElement(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_EVENT)
    public static class LogEventWithAdditionalFields {

        private final Object logEvent;
        private final Map<String, String> additionalFields;

        public LogEventWithAdditionalFields(final Object logEvent, final Map<String, String> additionalFields) {
            this.logEvent = logEvent;
            this.additionalFields = additionalFields;
        }

        @JsonUnwrapped
        public Object getLogEvent() {
            return logEvent;
        }

        @JsonAnyGetter
        @SuppressWarnings("unused")
        public Map<String, String> getAdditionalFields() {
            return additionalFields;
        }
    }

    protected static class ResolvableKeyValuePair {

        /**
         * The empty array.
         */
        static final ResolvableKeyValuePair[] EMPTY_ARRAY = {};

        final String key;
        final String value;
        final boolean valueNeedsLookup;

        ResolvableKeyValuePair(final KeyValuePair pair) {
            this.key = pair.getKey();
            this.value = pair.getValue();
            this.valueNeedsLookup = AbstractJacksonLayout.valueNeedsLookup(this.value);
        }
    }

    private static class ReadOnlyLogEventWrapper implements LogEvent {

        @JsonIgnore
        private final LogEvent event;

        public ReadOnlyLogEventWrapper(final LogEvent event) {
            this.event = event;
        }

        @Override
        public LogEvent toImmutable() {
            return event.toImmutable();
        }

        @Override
        public Map<String, String> getContextMap() {
            return event.getContextMap();
        }

        @Override
        public ReadOnlyStringMap getContextData() {
            return event.getContextData();
        }

        @Override
        public ThreadContext.ContextStack getContextStack() {
            return event.getContextStack();
        }

        @Override
        public String getLoggerFqcn() {
            return event.getLoggerFqcn();
        }

        @Override
        public Level getLevel() {
            return event.getLevel();
        }

        @Override
        public String getLoggerName() {
            return event.getLoggerName();
        }

        @Override
        public Marker getMarker() {
            return event.getMarker();
        }

        @Override
        public Message getMessage() {
            return event.getMessage();
        }

        @Override
        public long getTimeMillis() {
            return event.getTimeMillis();
        }

        @Override
        public Instant getInstant() {
            return event.getInstant();
        }

        @Override
        public StackTraceElement getSource() {
            return event.getSource();
        }

        @Override
        public String getThreadName() {
            return event.getThreadName();
        }

        @Override
        public long getThreadId() {
            return event.getThreadId();
        }

        @Override
        public int getThreadPriority() {
            return event.getThreadPriority();
        }

        @Override
        public Throwable getThrown() {
            return event.getThrown();
        }

        @Override
        public ThrowableProxy getThrownProxy() {
            return event.getThrownProxy();
        }

        @Override
        public boolean isEndOfBatch() {
            return event.isEndOfBatch();
        }

        @Override
        public boolean isIncludeLocation() {
            return event.isIncludeLocation();
        }

        @Override
        public void setEndOfBatch(final boolean endOfBatch) {}

        @Override
        public void setIncludeLocation(final boolean locationRequired) {}

        @Override
        public long getNanoTime() {
            return event.getNanoTime();
        }
    }
}

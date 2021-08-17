package org.apache.logging.log4j.jackson;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.jackson.model.DefaultLogEventWrapper;
import org.apache.logging.log4j.jackson.model.JsonModelLogEventWrapper;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractEnhancedJacksonLayout extends AbstractStringLayout {
    protected static final String DEFAULT_EOL = "\r\n";
    protected static final String COMPACT_EOL = "";
    protected final String eol;
    protected final ObjectWriter objectWriter;
    protected final boolean compact;
    protected final boolean complete;
    protected final Class<? extends JsonModelLogEventWrapper> modelClass;
    protected final boolean includeNullDelimiter;
    protected final AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[] additionalFields;

    /**
     * @deprecated
     */
    @Deprecated
    protected AbstractEnhancedJacksonLayout(Configuration config, ObjectWriter objectWriter, Charset charset, boolean compact, boolean complete, boolean eventEol, Serializer headerSerializer, Serializer footerSerializer, Class<? extends JsonModelLogEventWrapper> modelClass) {
        this(config, objectWriter, charset, compact, complete, eventEol, headerSerializer, footerSerializer, false, modelClass);
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected AbstractEnhancedJacksonLayout(Configuration config, ObjectWriter objectWriter, Charset charset, boolean compact, boolean complete, boolean eventEol, Serializer headerSerializer, Serializer footerSerializer, boolean includeNullDelimiter, Class<? extends JsonModelLogEventWrapper> modelClass) {
        this(config, objectWriter, charset, compact, complete, eventEol, null, headerSerializer, footerSerializer, includeNullDelimiter, null, modelClass);
    }

    protected AbstractEnhancedJacksonLayout(Configuration config, ObjectWriter objectWriter, Charset charset, boolean compact, boolean complete, boolean eventEol, String endOfLine, Serializer headerSerializer, Serializer footerSerializer, boolean includeNullDelimiter, KeyValuePair[] additionalFields, Class<? extends JsonModelLogEventWrapper> modelClass) {
        super(config, charset, headerSerializer, footerSerializer);
        this.objectWriter = objectWriter;
        this.compact = compact;
        this.complete = complete;
        this.eol = endOfLine != null ? endOfLine : (compact && !eventEol ? COMPACT_EOL : DEFAULT_EOL);
        this.includeNullDelimiter = includeNullDelimiter;
        this.additionalFields = prepareAdditionalFields(config, additionalFields);
        if (modelClass != null) {
            this.modelClass = modelClass;
        } else {
            this.modelClass = DefaultLogEventWrapper.class;
        }
    }

    protected static boolean valueNeedsLookup(String value) {
        return value != null && value.contains("${");
    }

    private static AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[] prepareAdditionalFields(Configuration config, KeyValuePair[] additionalFields) {
        if (additionalFields != null && additionalFields.length != 0) {
            AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[] resolvableFields = new AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[additionalFields.length];

            for (int i = 0; i < additionalFields.length; ++i) {
                AbstractEnhancedJacksonLayout.ResolvableKeyValuePair resolvable = resolvableFields[i] = new AbstractEnhancedJacksonLayout.ResolvableKeyValuePair(additionalFields[i]);
                if (config == null && resolvable.valueNeedsLookup) {
                    throw new IllegalArgumentException("configuration needs to be set when there are additional fields with variables");
                }
            }

            return resolvableFields;
        } else {
            return new AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[0];
        }
    }

    public String toSerializable(LogEvent event) {
        StringBuilderWriter writer = new StringBuilderWriter();

        try {
            this.toSerializable(event, writer);
            return writer.toString();
        } catch (Exception var4) {
            LOGGER.error(var4);
            return "";
        }
    }

    private static LogEvent convertMutableToLog4jEvent(LogEvent event) {
        return event instanceof Log4jLogEvent ? event : Log4jLogEvent.createMemento(event);
    }

    protected Object wrapLogEvent(LogEvent event) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, String> additionalFieldsMap = this.resolveAdditionalFields(event);
        return this.modelClass.getConstructor(LogEvent.class, Map.class).newInstance(event, additionalFieldsMap);
    }

    private Map<String, String> resolveAdditionalFields(LogEvent logEvent) {
        if (this.additionalFields.length <= 0) {
            return Collections.emptyMap();
        }

        Map<String, String> additionalFieldsMap = new LinkedHashMap(this.additionalFields.length);
        StrSubstitutor strSubstitutor = this.configuration.getStrSubstitutor();
        AbstractEnhancedJacksonLayout.ResolvableKeyValuePair[] var4 = this.additionalFields;
        int var5 = var4.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            AbstractEnhancedJacksonLayout.ResolvableKeyValuePair pair = var4[var6];
            if (pair.valueNeedsLookup) {
                additionalFieldsMap.put(pair.key, strSubstitutor.replace(logEvent, pair.value));
            } else {
                additionalFieldsMap.put(pair.key, pair.value);
            }
        }

        return additionalFieldsMap;
    }

    public void toSerializable(LogEvent event, Writer writer) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.objectWriter.writeValue(writer, this.wrapLogEvent(convertMutableToLog4jEvent(event)));
        writer.write(this.eol);
        if (this.includeNullDelimiter) {
            writer.write(0);
        }

        this.markEvent();
    }

    protected static class ResolvableKeyValuePair {
        final String key;
        final String value;
        final boolean valueNeedsLookup;

        ResolvableKeyValuePair(KeyValuePair pair) {
            this.key = pair.getKey();
            this.value = pair.getValue();
            this.valueNeedsLookup = AbstractEnhancedJacksonLayout.valueNeedsLookup(this.value);
        }
    }

    public abstract static class Builder<B extends AbstractEnhancedJacksonLayout.Builder<B>> extends org.apache.logging.log4j.core.layout.AbstractStringLayout.Builder<B> {
        @PluginBuilderAttribute
        private boolean eventEol;
        @PluginBuilderAttribute
        private String endOfLine;
        @PluginBuilderAttribute
        private boolean compact;
        @PluginBuilderAttribute
        private boolean complete;
        @PluginBuilderAttribute
        private Class<? extends JsonModelLogEventWrapper> model;
        @PluginBuilderAttribute
        private boolean includeNullDelimiter = false;
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        public Builder() {
        }

        protected String toStringOrNull(byte[] header) {
            return header == null ? null : new String(header, Charset.defaultCharset());
        }

        public boolean getEventEol() {
            return this.eventEol;
        }

        public String getEndOfLine() {
            return this.endOfLine;
        }

        public boolean isCompact() {
            return this.compact;
        }

        public boolean isComplete() {
            return this.complete;
        }

        public boolean isIncludeNullDelimiter() {
            return this.includeNullDelimiter;
        }

        public KeyValuePair[] getAdditionalFields() {
            return this.additionalFields;
        }

        public Class<? extends JsonModelLogEventWrapper> getModel() {
            return model;
        }

        public B setEventEol(boolean eventEol) {
            this.eventEol = eventEol;
            return this.asBuilder();
        }

        public B setEndOfLine(String endOfLine) {
            this.endOfLine = endOfLine;
            return this.asBuilder();
        }

        public B setCompact(boolean compact) {
            this.compact = compact;
            return this.asBuilder();
        }

        public B setComplete(boolean complete) {
            this.complete = complete;
            return this.asBuilder();
        }

        public B setIncludeNullDelimiter(boolean includeNullDelimiter) {
            this.includeNullDelimiter = includeNullDelimiter;
            return this.asBuilder();
        }

        public B setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return this.asBuilder();
        }

        public B setModelClass(Class<? extends JsonModelLogEventWrapper> model) {
            this.model = model;
            return this.asBuilder();
        }
    }
}



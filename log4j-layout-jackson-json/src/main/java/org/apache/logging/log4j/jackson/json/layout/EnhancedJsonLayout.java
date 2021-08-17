package org.apache.logging.log4j.jackson.json.layout;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.jackson.AbstractEnhancedJacksonLayout;
import org.apache.logging.log4j.jackson.model.DefaultLogEventWrapper;
import org.apache.logging.log4j.jackson.model.JsonModelLogEventWrapper;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Plugin(name = "EnhancedJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class EnhancedJsonLayout extends AbstractEnhancedJacksonLayout {
    private static final String DEFAULT_HEADER = "[";
    private static final String DEFAULT_FOOTER = "]";
    private static final String CONTENT_TYPE = "application/json";

    /**
     * @deprecated
     */
    @Deprecated
    protected EnhancedJsonLayout(Configuration config, boolean complete, boolean compact, boolean eventEol, String endOfLine, String headerPattern, String footerPattern, Charset charset, Class<? extends JsonModelLogEventWrapper> modelClass) {
        super(config, (new EnhancedJsonJacksonFactory()).newWriter(compact), charset, compact, complete, eventEol, endOfLine, PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(), PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(), false, null, modelClass);
    }

    private EnhancedJsonLayout(Configuration config, boolean complete, boolean compact, boolean eventEol, String endOfLine, String headerPattern, String footerPattern, Charset charset, boolean includeNullDelimiter, KeyValuePair[] additionalFields, Class<? extends JsonModelLogEventWrapper> modelClass) {
        super(config, (new EnhancedJsonJacksonFactory()).newWriter(compact), charset, compact, complete, eventEol, endOfLine, PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(), PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(), includeNullDelimiter, additionalFields, modelClass);
    }

    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        } else {
            StringBuilder buf = new StringBuilder();
            String str = this.serializeToString(this.getHeaderSerializer());
            if (str != null) {
                buf.append(str);
            }

            buf.append(this.eol);
            return this.getBytes(buf.toString());
        }
    }

    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(this.eol);
            String str = this.serializeToString(this.getFooterSerializer());
            if (str != null) {
                buf.append(str);
            }

            buf.append(this.eol);
            return this.getBytes(buf.toString());
        }
    }

    public Map<String, String> getContentFormat() {
        Map<String, String> result = new HashMap();
        result.put("version", "2.0");
        return result;
    }

    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static EnhancedJsonLayout createLayout(Configuration config, boolean complete, boolean compact, boolean eventEol, String headerPattern, String footerPattern, Charset charset, Class<? extends JsonModelLogEventWrapper> modelClass) {
        return new EnhancedJsonLayout(config, complete, compact, eventEol, null, headerPattern, footerPattern, charset, modelClass);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static EnhancedJsonLayout createDefaultLayout() {
        return new EnhancedJsonLayout(new DefaultConfiguration(), false, false, false, null, DEFAULT_HEADER, DEFAULT_FOOTER, StandardCharsets.UTF_8, false, null, DefaultLogEventWrapper.class);
    }

    public void toSerializable(LogEvent event, Writer writer) throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (this.complete && this.eventCount > 0L) {
            writer.append(", ");
        }

        super.toSerializable(event, writer);
    }

    public static class Builder<B extends EnhancedJsonLayout.Builder<B>> extends AbstractEnhancedJacksonLayout.Builder<B> implements org.apache.logging.log4j.core.util.Builder<EnhancedJsonLayout> {
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        public Builder() {
            this.setCharset(StandardCharsets.UTF_8);
        }

        public EnhancedJsonLayout build() {
            String headerPattern = this.toStringOrNull(this.getHeader());
            String footerPattern = this.toStringOrNull(this.getFooter());
            return new EnhancedJsonLayout(this.getConfiguration(), this.isComplete(), this.isCompact(), this.getEventEol(), this.getEndOfLine(), headerPattern, footerPattern, this.getCharset(), this.isIncludeNullDelimiter(), this.getAdditionalFields(), this.getModel());
        }

        public KeyValuePair[] getAdditionalFields() {
            return this.additionalFields;
        }

        public B setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return this.asBuilder();
        }
    }
}

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
package org.apache.logging.log4j.core.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Lays out events in the Graylog Extended Log Format (GELF) 1.1.
 * <p>
 * This layout compresses JSON to GZIP or ZLIB (the {@code compressionType}) if
 * log event data is larger than 1024 bytes (the {@code compressionThreshold}).
 * This layout does not implement chunking.
 * </p>
 * <p>
 * Configure as follows to send to a Graylog2 server:
 * </p>
 *
 * <pre>
 * &lt;Appenders&gt;
 *        &lt;Socket name="Graylog" protocol="udp" host="graylog.domain.com" port="12201"&gt;
 *            &lt;GelfLayout host="someserver" compressionType="GZIP" compressionThreshold="1024"&gt;
 *                &lt;KeyValuePair key="additionalField1" value="additional value 1"/&gt;
 *                &lt;KeyValuePair key="additionalField2" value="additional value 2"/&gt;
 *            &lt;/GelfLayout&gt;
 *        &lt;/Socket&gt;
 * &lt;/Appenders&gt;
 * </pre>
 *
 * @see <a href="http://docs.graylog.org/en/latest/pages/gelf.html#gelf">GELF specification</a>
 */
@Plugin(name = "GelfLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class GelfLayout extends AbstractStringLayout {

    public enum CompressionType {

        GZIP {
            @Override
            public DeflaterOutputStream createDeflaterOutputStream(final OutputStream os) throws IOException {
                return new GZIPOutputStream(os);
            }
        },
        ZLIB {
            @Override
            public DeflaterOutputStream createDeflaterOutputStream(final OutputStream os) throws IOException {
                return new DeflaterOutputStream(os);
            }
        },
        OFF {
            @Override
            public DeflaterOutputStream createDeflaterOutputStream(final OutputStream os) throws IOException {
                return null;
            }
        };

        public abstract DeflaterOutputStream createDeflaterOutputStream(OutputStream os) throws IOException;
    }

    private static final char C = ',';
    private static final int COMPRESSION_THRESHOLD = 1024;
    private static final char Q = '\"';
    private static final String QC = "\",";
    private static final String QU = "\"_";

    private final KeyValuePair[] additionalFields;
    private final int compressionThreshold;
    private final CompressionType compressionType;
    private final String host;
    private final boolean includeStacktrace;
    private final boolean includeThreadContext;

    public static class Builder<B extends Builder<B>> extends AbstractStringLayout.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<GelfLayout> {

        @PluginBuilderAttribute
        private String host;

        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        @PluginBuilderAttribute
        private CompressionType compressionType = CompressionType.GZIP;

        @PluginBuilderAttribute
        private int compressionThreshold = COMPRESSION_THRESHOLD;

        @PluginBuilderAttribute
        private boolean includeStacktrace = true;

        @PluginBuilderAttribute
        private boolean includeThreadContext = true;

        public Builder() {
            super();
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public GelfLayout build() {
            return new GelfLayout(getConfiguration(), host, additionalFields, compressionType, compressionThreshold, includeStacktrace, includeThreadContext);
        }

        public String getHost() {
            return host;
        }

        public CompressionType getCompressionType() {
            return compressionType;
        }

        public int getCompressionThreshold() {
            return compressionThreshold;
        }

        public boolean isIncludeStacktrace() {
            return includeStacktrace;
        }

        public boolean isIncludeThreadContext() {
            return includeThreadContext;
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        /**
         * The value of the <code>host</code> property (optional, defaults to local host name).
         *
         * @return this builder
         */
        public B setHost(String host) {
            this.host = host;
            return asBuilder();
        }

        /**
         * Compression to use (optional, defaults to GZIP).
         *
         * @return this builder
         */
        public B setCompressionType(CompressionType compressionType) {
            this.compressionType = compressionType;
            return asBuilder();
        }

        /**
         * Compress if data is larger than this number of bytes (optional, defaults to 1024).
         *
         * @return this builder
         */
        public B setCompressionThreshold(int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
            return asBuilder();
        }

        /**
         * Whether to include full stacktrace of logged Throwables (optional, default to true).
         * If set to false, only the class name and message of the Throwable will be included.
         *
         * @return this builder
         */
        public B setIncludeStacktrace(boolean includeStacktrace) {
            this.includeStacktrace = includeStacktrace;
            return asBuilder();
        }

        /**
         * Whether to include thread context as additional fields (optional, default to true).
         *
         * @return this builder
         */
        public B setIncludeThreadContext(boolean includeThreadContext) {
            this.includeThreadContext = includeThreadContext;
            return asBuilder();
        }

        /**
         * Additional fields to set on each log event.
         *
         * @return this builder
         */
        public B setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return asBuilder();
        }
    }

    /**
     * @deprecated Use {@link #newBuilder()} instead
     */
    @Deprecated
    public GelfLayout(final String host, final KeyValuePair[] additionalFields, final CompressionType compressionType,
                      final int compressionThreshold, final boolean includeStacktrace) {
        this(null, host, additionalFields, compressionType, compressionThreshold, includeStacktrace, true);
    }

    private GelfLayout(final Configuration config, final String host, final KeyValuePair[] additionalFields, final CompressionType compressionType,
               final int compressionThreshold, final boolean includeStacktrace, final boolean includeThreadContext) {
        super(config, StandardCharsets.UTF_8, null, null);
        this.host = host != null ? host : NetUtils.getLocalHostname();
        this.additionalFields = additionalFields != null ? additionalFields : new KeyValuePair[0];
        if (config == null) {
            for (KeyValuePair additionalField : this.additionalFields) {
                if (valueNeedsLookup(additionalField.getValue())) {
                    throw new IllegalArgumentException("configuration needs to be set when there are additional fields with variables");
                }
            }
        }
        this.compressionType = compressionType;
        this.compressionThreshold = compressionThreshold;
        this.includeStacktrace = includeStacktrace;
        this.includeThreadContext = includeThreadContext;
    }

    /**
     * @deprecated Use {@link #newBuilder()} instead
     */
    @Deprecated
    public static GelfLayout createLayout(
            //@formatter:off
            @PluginAttribute("host") final String host,
            @PluginElement("AdditionalField") final KeyValuePair[] additionalFields,
            @PluginAttribute(value = "compressionType",
                defaultString = "GZIP") final CompressionType compressionType,
            @PluginAttribute(value = "compressionThreshold",
                defaultInt = COMPRESSION_THRESHOLD) final int compressionThreshold,
            @PluginAttribute(value = "includeStacktrace",
                defaultBoolean = true) final boolean includeStacktrace) {
            // @formatter:on
        return new GelfLayout(null, host, additionalFields, compressionType, compressionThreshold, includeStacktrace, true);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    @Override
    public Map<String, String> getContentFormat() {
        return Collections.emptyMap();
    }

    @Override
    public String getContentType() {
        return JsonLayout.CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        final StringBuilder text = toText(event, getStringBuilder(), false);
        final byte[] bytes = getBytes(text.toString());
        return compressionType != CompressionType.OFF && bytes.length > compressionThreshold ? compress(bytes) : bytes;
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        if (compressionType != CompressionType.OFF) {
            super.encode(event, destination);
            return;
        }
        final StringBuilder text = toText(event, getStringBuilder(), true);
        final Encoder<StringBuilder> helper = getStringBuilderEncoder();
        helper.encode(text, destination);
    }

    private byte[] compress(final byte[] bytes) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(compressionThreshold / 8);
            try (final DeflaterOutputStream stream = compressionType.createDeflaterOutputStream(baos)) {
                if (stream == null) {
                    return bytes;
                }
                stream.write(bytes);
                stream.finish();
            }
            return baos.toByteArray();
        } catch (final IOException e) {
            StatusLogger.getLogger().error(e);
            return bytes;
        }
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder text = toText(event, getStringBuilder(), false);
        return text.toString();
    }

    private StringBuilder toText(final LogEvent event, final StringBuilder builder, final boolean gcFree) {
        builder.append('{');
        builder.append("\"version\":\"1.1\",");
        builder.append("\"host\":\"");
        JsonUtils.quoteAsString(toNullSafeString(host), builder);
        builder.append(QC);
        builder.append("\"timestamp\":").append(formatTimestamp(event.getTimeMillis())).append(C);
        builder.append("\"level\":").append(formatLevel(event.getLevel())).append(C);
        if (event.getThreadName() != null) {
            builder.append("\"_thread\":\"");
            JsonUtils.quoteAsString(event.getThreadName(), builder);
            builder.append(QC);
        }
        if (event.getLoggerName() != null) {
            builder.append("\"_logger\":\"");
            JsonUtils.quoteAsString(event.getLoggerName(), builder);
            builder.append(QC);
        }
        if (additionalFields.length > 0) {
            final StrSubstitutor strSubstitutor = getConfiguration().getStrSubstitutor();
            for (final KeyValuePair additionalField : additionalFields) {
                builder.append(QU);
                JsonUtils.quoteAsString(additionalField.getKey(), builder);
                builder.append("\":\"");
                final String value = valueNeedsLookup(additionalField.getValue())
                    ? strSubstitutor.replace(event, additionalField.getValue())
                    : additionalField.getValue();
                JsonUtils.quoteAsString(toNullSafeString(value), builder);
                builder.append(QC);
            }
        }
        if (includeThreadContext) {
            event.getContextData().forEach(WRITE_KEY_VALUES_INTO, builder);
        }
        if (event.getThrown() != null) {
            builder.append("\"full_message\":\"");
            if (includeStacktrace) {
                JsonUtils.quoteAsString(formatThrowable(event.getThrown()), builder);
            } else {
                JsonUtils.quoteAsString(event.getThrown().toString(), builder);
            }
            builder.append(QC);
        }

        builder.append("\"short_message\":\"");
        final Message message = event.getMessage();
        if (message instanceof CharSequence) {
            JsonUtils.quoteAsString(((CharSequence)message), builder);
        } else if (gcFree && message instanceof StringBuilderFormattable) {
            final StringBuilder messageBuffer = getMessageStringBuilder();
            try {
                ((StringBuilderFormattable) message).formatTo(messageBuffer);
                JsonUtils.quoteAsString(messageBuffer, builder);
            } finally {
                trimToMaxSize(messageBuffer);
            }
        } else {
            JsonUtils.quoteAsString(toNullSafeString(message.getFormattedMessage()), builder);
        }
        builder.append(Q);
        builder.append('}');
        return builder;
    }

    private static boolean valueNeedsLookup(final String value) {
        return value != null && value.contains("${");
    }

    private static final TriConsumer<String, Object, StringBuilder> WRITE_KEY_VALUES_INTO = new TriConsumer<String, Object, StringBuilder>() {
        @Override
        public void accept(final String key, final Object value, final StringBuilder stringBuilder) {
            stringBuilder.append(QU);
            JsonUtils.quoteAsString(key, stringBuilder);
            stringBuilder.append("\":\"");
            JsonUtils.quoteAsString(toNullSafeString(String.valueOf(value)), stringBuilder);
            stringBuilder.append(QC);
        }
    };

    private static final ThreadLocal<StringBuilder> messageStringBuilder = new ThreadLocal<>();

    private static StringBuilder getMessageStringBuilder() {
        StringBuilder result = messageStringBuilder.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            messageStringBuilder.set(result);
        }
        result.setLength(0);
        return result;
    }

    private static CharSequence toNullSafeString(final CharSequence s) {
        return s == null ? Strings.EMPTY : s;
    }

    /**
     * Non-private to make it accessible from unit test.
     */
    static CharSequence formatTimestamp(final long timeMillis) {
        if (timeMillis < 1000) {
            return "0";
        }
        final StringBuilder builder = getTimestampStringBuilder();
        builder.append(timeMillis);
        builder.insert(builder.length() - 3, '.');
        return builder;
    }

    private static final ThreadLocal<StringBuilder> timestampStringBuilder = new ThreadLocal<>();

    private static StringBuilder getTimestampStringBuilder() {
        StringBuilder result = timestampStringBuilder.get();
        if (result == null) {
            result = new StringBuilder(20);
            timestampStringBuilder.set(result);
        }
        result.setLength(0);
        return result;
    }

    /**
     * http://en.wikipedia.org/wiki/Syslog#Severity_levels
     */
    private int formatLevel(final Level level) {
        return Severity.getSeverity(level).getCode();
    }

    /**
     * Non-private to make it accessible from unit test.
     */
    static CharSequence formatThrowable(final Throwable throwable) {
        // stack traces are big enough to provide a reasonably large initial capacity here
        final StringWriter sw = new StringWriter(2048);
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.getBuffer();
    }
}

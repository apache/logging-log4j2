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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

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
 * @see <a href="http://graylog2.org/resources/gelf/specification">GELF
 *      specification</a>
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

    public GelfLayout(final String host, final KeyValuePair[] additionalFields, final CompressionType compressionType,
            final int compressionThreshold) {
        super(StandardCharsets.UTF_8);
        this.host = host;
        this.additionalFields = additionalFields;
        this.compressionType = compressionType;
        this.compressionThreshold = compressionThreshold;
    }
    
    @PluginFactory
    public static GelfLayout createLayout(
            //@formatter:off
            @PluginAttribute("host") final String host,
            @PluginElement("AdditionalField") final KeyValuePair[] additionalFields,
            @PluginAttribute(value = "compressionType",
                defaultString = "GZIP") final CompressionType compressionType,
            @PluginAttribute(value = "compressionThreshold",
                defaultInt= COMPRESSION_THRESHOLD) final int compressionThreshold) {
            // @formatter:on
        return new GelfLayout(host, additionalFields, compressionType, compressionThreshold);
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
        StringBuilder text = toText(event, getStringBuilder(), false);
        final byte[] bytes = getBytes(text.toString());
        return compressionType != CompressionType.OFF && bytes.length > compressionThreshold ? compress(bytes) : bytes;
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        if (!Constants.ENABLE_DIRECT_ENCODERS || !Constants.ENABLE_THREADLOCALS || compressionType != CompressionType.OFF) {
            super.encode(event, destination);
            return;
        }
        final StringBuilder text = toText(event, getStringBuilder(), true);
        final TextEncoderHelper helper = getCachedTextEncoderHelper();
        helper.encodeText(text, destination);
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

    private StringBuilder toText(LogEvent event, StringBuilder builder, boolean gcFree) {
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

        for (final KeyValuePair additionalField : additionalFields) {
            builder.append(QU);
            JsonUtils.quoteAsString(additionalField.getKey(), builder);
            builder.append("\":\"");
            JsonUtils.quoteAsString(toNullSafeString(additionalField.getValue()), builder);
            builder.append(QC);
        }
        for (final Map.Entry<String, String> entry : event.getContextMap().entrySet()) {
            builder.append(QU);
            JsonUtils.quoteAsString(entry.getKey(), builder);
            builder.append("\":\"");
            JsonUtils.quoteAsString(toNullSafeString(entry.getValue()), builder);
            builder.append(QC);
        }
        if (event.getThrown() != null) {
            builder.append("\"full_message\":\"");
            JsonUtils.quoteAsString(formatThrowable(event.getThrown()), builder);
            builder.append(QC);
        }

        builder.append("\"short_message\":\"");
        Message message = event.getMessage();
        if (gcFree && message instanceof StringBuilderFormattable) {
            StringBuilder messageBuffer = getMessageStringBuilder();
            ((StringBuilderFormattable)message).formatTo(messageBuffer);
            JsonUtils.quoteAsString(messageBuffer, builder);
        } else {
            JsonUtils.quoteAsString(toNullSafeString(message.getFormattedMessage()), builder);
        }
        builder.append(Q);
        builder.append('}');
        return builder;
    }

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

    private CharSequence toNullSafeString(final CharSequence s) {
        return s == null ? Strings.EMPTY : s;
    }

    /**
     * Non-private to make it accessible from unit test.
     */
    static CharSequence formatTimestamp(final long timeMillis) {
        if (timeMillis < 1000) {
            return "0";
        } else {
            StringBuilder builder = getTimestampStringBuilder();
            builder.append(timeMillis);
            builder.insert(builder.length() - 3, '.');
            return builder;
        }
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
    static String formatThrowable(final Throwable throwable) {
        // stack traces are big enough to provide a reasonably large initial capacity here
        final StringWriter sw = new StringWriter(2048);
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}

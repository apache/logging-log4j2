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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.core.util.Charsets;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Lays out Graylog Extended Log Format (GELF) 1.1 log events.
 * <p>
 * GZIP compresses the JSON if log event data is larger than 1 KiB. Does not implement chunking.
 * </p>
 * <p>
 * Configure as follows to send to a Graylog2 server:
 * </p>
 * 
 * <pre>
 * &lt;Appenders&gt;
 *        &lt;Socket name="Graylog" protocol="udp" host="graylog.domain.com" port="12201"&gt;
 *            &lt;GelfLayout host="someserver"&gt;
 *                &lt;KeyValuePair key="additionalField1" value="additional value 1"/&gt;
 *                &lt;KeyValuePair key="additionalField2" value="additional value 2"/&gt;
 *            &lt;/GelfLayout&gt;
 *        &lt;/Socket&gt;
 * &lt;/Appenders&gt;
 * </pre>
 *
 * @see http://graylog2.org/gelf
 * @see http://graylog2.org/resources/gelf/specification
 */
@Plugin(name = "GelfLayout", category = "Core", elementType = "layout", printObject = true)
public final class GelfLayout extends AbstractStringLayout {

    public static enum CompressionType {
        GZIP, ZLIB, NONE
    }

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final char C = ',';
    private static final int COMPRESSION_THRESHOLD = 1024;
    private static final char Q = '\"';
    private static final String QC = "\",";
    private static final String QU = "\"_";
    private static final long serialVersionUID = 1L;
    private static final BigDecimal TIME_DIVISOR = new BigDecimal(1000);

    @PluginFactory
    public static GelfLayout createLayout( 
            //@formatter:off            
            @PluginAttribute("host") final String host,
            @PluginElement("AdditionalField") final KeyValuePair[] additionalFields,
            @PluginAttribute(value = "compressionThreshold", 
                defaultString = "GZIP") final CompressionType compressionType,
            @PluginAttribute(value = "compressionThreshold", 
                defaultInt= COMPRESSION_THRESHOLD) final int compressionThreshold) {
            // @formatter:on
        return new GelfLayout(host, additionalFields, compressionType, compressionThreshold);
    }

    static String formatTimestamp(final long timeMillis) {
        return new BigDecimal(timeMillis).divide(TIME_DIVISOR).toPlainString();
    }

    private final KeyValuePair[] additionalFields;

    private final String host;

    private final int compressionThreshold;

    private CompressionType compressionType;

    public GelfLayout(final String host, final KeyValuePair[] additionalFields, CompressionType compressionType,
            final int compressionThreshold) {
        super(Charsets.UTF_8);
        this.host = host;
        this.additionalFields = additionalFields;
        this.compressionType = compressionType;
        this.compressionThreshold = compressionThreshold;
    }

    private byte[] compress(final byte[] bytes) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(compressionThreshold / 8);
            DeflaterOutputStream stream;
            switch (compressionType) {
            case GZIP:
                stream = new GZIPOutputStream(baos);
                break;
            case ZLIB:
                stream = new DeflaterOutputStream(baos);
                break;
            default:
                return bytes;
            }
            stream.write(bytes);
            stream.finish();
            stream.close();
            return baos.toByteArray();
        } catch (final IOException e) {
            StatusLogger.getLogger().error(e);
            return EMPTY_BYTES;
        }
    }

    private String escapeJson(final String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * http://en.wikipedia.org/wiki/Syslog#Severity_levels
     */
    private int formatLevel(final Level level) {
        return Severity.getSeverity(level).getCode();
    }

    private String formatThrowable(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
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
        final byte[] bytes = toSerializable(event).getBytes(getCharset());
        return bytes.length > compressionThreshold ? compress(bytes) : bytes;
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder builder = new StringBuilder(256);
        builder.append('{');
        builder.append("\"version\":\"1.1\",");
        builder.append("\"host\":\"").append(escapeJson(host)).append(QC);
        builder.append("\"timestamp\":").append(formatTimestamp(event.getTimeMillis())).append(C);
        builder.append("\"level\":").append(formatLevel(event.getLevel())).append(C);
        if (event.getThreadName() != null) {
            builder.append("\"_thread\":\"").append(escapeJson(event.getThreadName())).append(QC);
        }
        if (event.getLoggerName() != null) {
            builder.append("\"_logger\":\"").append(escapeJson(event.getLoggerName())).append(QC);
        }

        for (final KeyValuePair additionalField : additionalFields) {
            builder.append(QU).append(escapeJson(additionalField.getKey())).append("\":\"")
                    .append(escapeJson(additionalField.getValue())).append(QC);
        }
        for (final Map.Entry<String, String> entry : event.getContextMap().entrySet()) {
            builder.append(QU).append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue()))
                    .append(QC);
        }
        if (event.getThrown() != null) {
            builder.append("\"full_message\":\"").append(escapeJson(formatThrowable(event.getThrown()))).append(QC);
        }

        builder.append("\"short_message\":\"").append(escapeJson(event.getMessage().getFormattedMessage())).append(Q);
        builder.append('}');
        return builder.toString();
    }
}

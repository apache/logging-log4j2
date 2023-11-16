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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * The Syslog Appender.
 */
@Plugin(name = "Syslog", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SyslogAppender extends SocketAppender {

    public static class Builder<B extends Builder<B>> extends AbstractBuilder<B>
            implements org.apache.logging.log4j.core.util.Builder<SocketAppender> {

        @PluginBuilderAttribute(value = "facility")
        private Facility facility = Facility.LOCAL0;

        @PluginBuilderAttribute("id")
        private String id;

        @PluginBuilderAttribute(value = "enterpriseNumber")
        private String enterpriseNumber = String.valueOf(Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER);

        @PluginBuilderAttribute(value = "includeMdc")
        private boolean includeMdc = true;

        @PluginBuilderAttribute("mdcId")
        private String mdcId;

        @PluginBuilderAttribute("mdcPrefix")
        private String mdcPrefix;

        @PluginBuilderAttribute("eventPrefix")
        private String eventPrefix;

        @PluginBuilderAttribute(value = "newLine")
        private boolean newLine;

        @PluginBuilderAttribute("newLineEscape")
        private String escapeNL;

        @PluginBuilderAttribute("appName")
        private String appName;

        @PluginBuilderAttribute("messageId")
        private String msgId;

        @PluginBuilderAttribute("mdcExcludes")
        private String excludes;

        @PluginBuilderAttribute("mdcIncludes")
        private String includes;

        @PluginBuilderAttribute("mdcRequired")
        private String required;

        @PluginBuilderAttribute("format")
        private String format;

        @PluginBuilderAttribute("charset")
        private Charset charsetName = StandardCharsets.UTF_8;

        @PluginBuilderAttribute("exceptionPattern")
        private String exceptionPattern;

        @PluginElement("LoggerFields")
        private LoggerFields[] loggerFields;

        @SuppressWarnings({"resource"})
        @Override
        public SyslogAppender build() {
            final Protocol protocol = getProtocol();
            final SslConfiguration sslConfiguration = getSslConfiguration();
            final boolean useTlsMessageFormat = sslConfiguration != null || protocol == Protocol.SSL;
            final Configuration configuration = getConfiguration();
            Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                layout = RFC5424.equalsIgnoreCase(format)
                        ? new Rfc5424Layout.Rfc5424LayoutBuilder()
                                .setFacility(facility)
                                .setId(id)
                                .setEin(enterpriseNumber)
                                .setIncludeMDC(includeMdc)
                                .setMdcId(mdcId)
                                .setMdcPrefix(mdcPrefix)
                                .setEventPrefix(eventPrefix)
                                .setIncludeNL(newLine)
                                .setEscapeNL(escapeNL)
                                .setAppName(appName)
                                .setMessageId(msgId)
                                .setExcludes(excludes)
                                .setIncludes(includes)
                                .setRequired(required)
                                .setExceptionPattern(exceptionPattern)
                                .setUseTLSMessageFormat(useTlsMessageFormat)
                                .setLoggerFields(loggerFields)
                                .setConfig(configuration)
                                .build()
                        :
                        // @formatter:off
                        SyslogLayout.newBuilder()
                                .setFacility(facility)
                                .setIncludeNewLine(newLine)
                                .setEscapeNL(escapeNL)
                                .setCharset(charsetName)
                                .build();
                // @formatter:on
            }
            final String name = getName();
            if (name == null) {
                LOGGER.error("No name provided for SyslogAppender");
                return null;
            }
            final AbstractSocketManager manager = createSocketManager(
                    name,
                    protocol,
                    getHost(),
                    getPort(),
                    getConnectTimeoutMillis(),
                    sslConfiguration,
                    getReconnectDelayMillis(),
                    getImmediateFail(),
                    layout,
                    Constants.ENCODER_BYTE_BUFFER_SIZE,
                    getSocketOptions());

            return new SyslogAppender(
                    name,
                    layout,
                    getFilter(),
                    isIgnoreExceptions(),
                    isImmediateFlush(),
                    manager,
                    getAdvertise() ? configuration.getAdvertiser() : null,
                    null);
        }

        public Facility getFacility() {
            return facility;
        }

        public String getId() {
            return id;
        }

        public String getEnterpriseNumber() {
            return enterpriseNumber;
        }

        public boolean isIncludeMdc() {
            return includeMdc;
        }

        public String getMdcId() {
            return mdcId;
        }

        public String getMdcPrefix() {
            return mdcPrefix;
        }

        public String getEventPrefix() {
            return eventPrefix;
        }

        public boolean isNewLine() {
            return newLine;
        }

        public String getEscapeNL() {
            return escapeNL;
        }

        public String getAppName() {
            return appName;
        }

        public String getMsgId() {
            return msgId;
        }

        public String getExcludes() {
            return excludes;
        }

        public String getIncludes() {
            return includes;
        }

        public String getRequired() {
            return required;
        }

        public String getFormat() {
            return format;
        }

        public Charset getCharsetName() {
            return charsetName;
        }

        public String getExceptionPattern() {
            return exceptionPattern;
        }

        public LoggerFields[] getLoggerFields() {
            return loggerFields;
        }

        public B setFacility(final Facility facility) {
            this.facility = facility;
            return asBuilder();
        }

        public B setId(final String id) {
            this.id = id;
            return asBuilder();
        }

        public B setEnterpriseNumber(final String enterpriseNumber) {
            this.enterpriseNumber = enterpriseNumber;
            return asBuilder();
        }

        /**
         * @deprecated Use {@link #setEnterpriseNumber(String)} instead
         */
        public B setEnterpriseNumber(final int enterpriseNumber) {
            this.enterpriseNumber = String.valueOf(enterpriseNumber);
            return asBuilder();
        }

        public B setIncludeMdc(final boolean includeMdc) {
            this.includeMdc = includeMdc;
            return asBuilder();
        }

        public B setMdcId(final String mdcId) {
            this.mdcId = mdcId;
            return asBuilder();
        }

        public B setMdcPrefix(final String mdcPrefix) {
            this.mdcPrefix = mdcPrefix;
            return asBuilder();
        }

        public B setEventPrefix(final String eventPrefix) {
            this.eventPrefix = eventPrefix;
            return asBuilder();
        }

        public B setNewLine(final boolean newLine) {
            this.newLine = newLine;
            return asBuilder();
        }

        public B setEscapeNL(final String escapeNL) {
            this.escapeNL = escapeNL;
            return asBuilder();
        }

        public B setAppName(final String appName) {
            this.appName = appName;
            return asBuilder();
        }

        public B setMsgId(final String msgId) {
            this.msgId = msgId;
            return asBuilder();
        }

        public B setExcludes(final String excludes) {
            this.excludes = excludes;
            return asBuilder();
        }

        public B setIncludes(final String includes) {
            this.includes = includes;
            return asBuilder();
        }

        public B setRequired(final String required) {
            this.required = required;
            return asBuilder();
        }

        public B setFormat(final String format) {
            this.format = format;
            return asBuilder();
        }

        public B setCharsetName(final Charset charset) {
            this.charsetName = charset;
            return asBuilder();
        }

        public B setExceptionPattern(final String exceptionPattern) {
            this.exceptionPattern = exceptionPattern;
            return asBuilder();
        }

        public B setLoggerFields(final LoggerFields[] loggerFields) {
            this.loggerFields = loggerFields;
            return asBuilder();
        }
    }

    protected static final String RFC5424 = "RFC5424";

    protected SyslogAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final AbstractSocketManager manager,
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser, properties);
    }

    /**
     * @deprecated Use
     * {@link #SyslogAppender(String, Layout, Filter, boolean, boolean, AbstractSocketManager, Advertiser, Property[])}.
     */
    @Deprecated
    protected SyslogAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final AbstractSocketManager manager,
            final Advertiser advertiser) {
        super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser, Property.EMPTY_ARRAY);
    }

    /**
     * Creates a SyslogAppender.
     * @param host The name of the host to connect to.
     * @param port The port to connect to on the target host.
     * @param protocolStr The Protocol to use.
     * @param sslConfiguration TODO
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * @param reconnectDelayMillis The interval in which failed writes should be retried.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
     *                         otherwise they are propagated to the caller.
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param enterpriseNumber The IANA enterprise number.
     * @param includeMdc Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     * record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param newLine If true, a newline will be appended to the end of the syslog record. The default is false.
     * @param escapeNL String that should be used to replace newlines within the message text.
     * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
     * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
     * @param excludes A comma separated list of mdc keys that should be excluded from the LogEvent.
     * @param includes A comma separated list of mdc keys that should be included in the FlumeEvent.
     * @param required A comma separated list of mdc keys that must be present in the MDC.
     * @param format If set to "RFC5424" the data will be formatted in accordance with RFC 5424. Otherwise,
     * it will be formatted as a BSD Syslog record.
     * @param filter A Filter to determine if the event should be handled by this Appender.
     * @param configuration The Configuration.
     * @param charset The character set to use when converting the syslog String to a byte array.
     * @param exceptionPattern The converter pattern to use for formatting exceptions.
     * @param loggerFields The logger fields
     * @param advertise Whether to advertise
     * @return A SyslogAppender.
     * @deprecated Use {@link #newSyslogAppenderBuilder()}.
     */
    @Deprecated
    public static <B extends Builder<B>> SyslogAppender createAppender(
            // @formatter:off
            final String host,
            final int port,
            final String protocolStr,
            final SslConfiguration sslConfiguration,
            final int connectTimeoutMillis,
            final int reconnectDelayMillis,
            final boolean immediateFail,
            final String name,
            final boolean immediateFlush,
            final boolean ignoreExceptions,
            final Facility facility,
            final String id,
            final int enterpriseNumber,
            final boolean includeMdc,
            final String mdcId,
            final String mdcPrefix,
            final String eventPrefix,
            final boolean newLine,
            final String escapeNL,
            final String appName,
            final String msgId,
            final String excludes,
            final String includes,
            final String required,
            final String format,
            final Filter filter,
            final Configuration configuration,
            final Charset charset,
            final String exceptionPattern,
            final LoggerFields[] loggerFields,
            final boolean advertise) {
        // @formatter:on

        // @formatter:off
        return SyslogAppender.<B>newSyslogAppenderBuilder()
                .setHost(host)
                .setPort(port)
                .setProtocol(EnglishEnums.valueOf(Protocol.class, protocolStr))
                .setSslConfiguration(sslConfiguration)
                .setConnectTimeoutMillis(connectTimeoutMillis)
                .setReconnectDelayMillis(reconnectDelayMillis)
                .setImmediateFail(immediateFail)
                .setName(appName)
                .setImmediateFlush(immediateFlush)
                .setIgnoreExceptions(ignoreExceptions)
                .setFilter(filter)
                .setConfiguration(configuration)
                .setAdvertise(advertise)
                .setFacility(facility)
                .setId(id)
                .setEnterpriseNumber(enterpriseNumber)
                .setIncludeMdc(includeMdc)
                .setMdcId(mdcId)
                .setMdcPrefix(mdcPrefix)
                .setEventPrefix(eventPrefix)
                .setNewLine(newLine)
                .setAppName(appName)
                .setMsgId(msgId)
                .setExcludes(excludes)
                .setIncludeMdc(includeMdc)
                .setRequired(required)
                .setFormat(format)
                .setCharsetName(charset)
                .setExceptionPattern(exceptionPattern)
                .setLoggerFields(loggerFields)
                .build();
        // @formatter:on
    }

    // Calling this method newBuilder() does not compile
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newSyslogAppenderBuilder() {
        return new Builder<B>().asBuilder();
    }
}

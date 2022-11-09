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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * The Syslog Appender.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("Syslog")
public class SyslogAppender extends SocketAppender {

    public static class Builder<B extends Builder<B>> extends AbstractBuilder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<SocketAppender> {

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
                        .build() :
                        // @formatter:off
                        SyslogLayout.newBuilder()
                            .setFacility(facility)
                            .setIncludeNewLine(newLine)
                            .setEscapeNL(escapeNL)
                            .setCharset(charsetName)
                            .build();
                        // @formatter:off
            }
            final String name = getName();
            if (name == null) {
                LOGGER.error("No name provided for SyslogAppender");
                return null;
            }
            final AbstractSocketManager manager = createSocketManager(name, protocol, getHost(), getPort(), getConnectTimeoutMillis(),
                    sslConfiguration, getReconnectDelayMillis(), getImmediateFail(), layout, Constants.ENCODER_BYTE_BUFFER_SIZE, null);

            return new SyslogAppender(name, layout, getFilter(), isIgnoreExceptions(), isImmediateFlush(), manager,
                    getAdvertise() ? configuration.getAdvertiser() : null);
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

    protected SyslogAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                             final boolean ignoreExceptions, final boolean immediateFlush,
                             final AbstractSocketManager manager, final Advertiser advertiser) {
        super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser);

    }

    // Calling this method newBuilder() does not compile
    @PluginFactory
    public static <B extends Builder<B>> B newSyslogAppenderBuilder() {
        return new Builder<B>().asBuilder();
    }

}

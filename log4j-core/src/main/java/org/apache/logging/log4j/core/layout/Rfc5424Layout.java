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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.TlsSyslogFrame;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.internal.ExcludeChecker;
import org.apache.logging.log4j.core.layout.internal.IncludeChecker;
import org.apache.logging.log4j.core.layout.internal.ListChecker;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageCollectionMessage;
import org.apache.logging.log4j.message.StructuredDataCollectionMessage;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.ProcessIdUtil;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

/**
 * Formats a log event in accordance with RFC 5424.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5424">RFC 5424</a>
 */
@Plugin(name = "Rfc5424Layout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class Rfc5424Layout extends AbstractStringLayout {

    /**
     * The default example enterprise number from RFC5424.
     */
    public static final int DEFAULT_ENTERPRISE_NUMBER = 32473;
    /**
     * The default event id.
     */
    public static final String DEFAULT_ID = "Audit";
    /**
     * Match newlines in a platform-independent manner.
     */
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
    /**
     * Match characters which require escaping.
     */
    @Deprecated
    public static final Pattern PARAM_VALUE_ESCAPE_PATTERN = Pattern.compile("[\\\"\\]\\\\]");

    /**
     * For now, avoid too restrictive OID checks to allow for easier transition
     */
    public static final Pattern ENTERPRISE_ID_PATTERN = Pattern.compile("\\d+(\\.\\d+)*");

    /**
     * Default MDC ID: {@value} .
     */
    public static final String DEFAULT_MDCID = "mdc";

    private static final String LF = "\n";
    private static final int TWO_DIGITS = 10;
    private static final int THREE_DIGITS = 100;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int MINUTES_PER_HOUR = 60;
    private static final String COMPONENT_KEY = "RFC5424-Converter";

    private final Facility facility;
    private final String defaultId;
    private final String enterpriseNumber;
    private final boolean includeMdc;
    private final String mdcId;
    private final StructuredDataId mdcSdId;
    private final String localHostName;
    private final String appName;
    private final String messageId;
    private final String configName;
    private final String mdcPrefix;
    private final String eventPrefix;
    private final List<String> mdcExcludes;
    private final List<String> mdcIncludes;
    private final List<String> mdcRequired;
    private final ListChecker listChecker;
    private final boolean includeNewLine;
    private final String escapeNewLine;
    private final boolean useTlsMessageFormat;

    private long lastTimestamp = -1;
    private String timestamppStr;

    private final List<PatternFormatter> exceptionFormatters;
    private final Map<String, FieldFormatter> fieldFormatters;
    private final String procId;

    private Rfc5424Layout(
            final Configuration config,
            final Facility facility,
            final String id,
            final String ein,
            final boolean includeMDC,
            final boolean includeNL,
            final String escapeNL,
            final String mdcId,
            final String mdcPrefix,
            final String eventPrefix,
            final String appName,
            final String messageId,
            final String excludes,
            final String includes,
            final String required,
            final Charset charset,
            final String exceptionPattern,
            final boolean useTLSMessageFormat,
            final LoggerFields[] loggerFields) {
        super(charset);
        final PatternParser exceptionParser = createPatternParser(config, ThrowablePatternConverter.class);
        exceptionFormatters = exceptionPattern == null ? null : exceptionParser.parse(exceptionPattern);
        this.facility = facility;
        this.defaultId = id == null ? DEFAULT_ID : id;
        this.enterpriseNumber = ein;
        this.includeMdc = includeMDC;
        this.includeNewLine = includeNL;
        this.escapeNewLine = escapeNL == null ? null : Matcher.quoteReplacement(escapeNL);
        this.mdcId = mdcId != null ? mdcId : id == null ? DEFAULT_MDCID : id;
        this.mdcSdId = new StructuredDataId(this.mdcId, enterpriseNumber, null, null);
        this.mdcPrefix = mdcPrefix;
        this.eventPrefix = eventPrefix;
        this.appName = appName;
        this.messageId = messageId;
        this.useTlsMessageFormat = useTLSMessageFormat;
        this.localHostName = NetUtils.getLocalHostname();
        ListChecker checker = null;
        if (excludes != null) {
            final String[] array = excludes.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                mdcExcludes = new ArrayList<>(array.length);
                for (final String str : array) {
                    mdcExcludes.add(str.trim());
                }
                checker = new ExcludeChecker(mdcExcludes);
            } else {
                mdcExcludes = null;
            }
        } else {
            mdcExcludes = null;
        }
        if (includes != null) {
            final String[] array = includes.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                mdcIncludes = new ArrayList<>(array.length);
                for (final String str : array) {
                    mdcIncludes.add(str.trim());
                }
                checker = new IncludeChecker(mdcIncludes);
            } else {
                mdcIncludes = null;
            }
        } else {
            mdcIncludes = null;
        }
        if (required != null) {
            final String[] array = required.split(Patterns.COMMA_SEPARATOR);
            if (array.length > 0) {
                mdcRequired = new ArrayList<>(array.length);
                for (final String str : array) {
                    mdcRequired.add(str.trim());
                }
            } else {
                mdcRequired = null;
            }

        } else {
            mdcRequired = null;
        }
        this.listChecker = checker != null ? checker : ListChecker.NOOP_CHECKER;
        final String name = config == null ? null : config.getName();
        configName = Strings.isNotEmpty(name) ? name : null;
        this.fieldFormatters = createFieldFormatters(loggerFields, config);
        this.procId = ProcessIdUtil.getProcessId();
    }

    private Map<String, FieldFormatter> createFieldFormatters(
            final LoggerFields[] loggerFields, final Configuration config) {
        final Map<String, FieldFormatter> sdIdMap = new HashMap<>(loggerFields == null ? 0 : loggerFields.length);
        if (loggerFields != null) {
            for (final LoggerFields loggerField : loggerFields) {
                final StructuredDataId key = loggerField.getSdId() == null ? mdcSdId : loggerField.getSdId();
                final Map<String, List<PatternFormatter>> sdParams = new HashMap<>();
                final Map<String, String> fields = loggerField.getMap();
                if (!fields.isEmpty()) {
                    final PatternParser fieldParser = createPatternParser(config, null);

                    for (final Map.Entry<String, String> entry : fields.entrySet()) {
                        final List<PatternFormatter> formatters = fieldParser.parse(entry.getValue());
                        sdParams.put(entry.getKey(), formatters);
                    }
                    final FieldFormatter fieldFormatter =
                            new FieldFormatter(sdParams, loggerField.getDiscardIfAllFieldsAreEmpty());
                    sdIdMap.put(key.toString(), fieldFormatter);
                }
            }
        }
        return sdIdMap.size() > 0 ? sdIdMap : null;
    }

    /**
     * Create a PatternParser.
     *
     * @param config The Configuration.
     * @param filterClass Filter the returned plugins after calling the plugin manager.
     * @return The PatternParser.
     */
    private static PatternParser createPatternParser(
            final Configuration config, final Class<? extends PatternConverter> filterClass) {
        if (config == null) {
            return new PatternParser(config, PatternLayout.KEY, LogEventPatternConverter.class, filterClass);
        }
        PatternParser parser = config.getComponent(COMPONENT_KEY);
        if (parser == null) {
            parser = new PatternParser(config, PatternLayout.KEY, ThrowablePatternConverter.class);
            config.addComponent(COMPONENT_KEY, parser);
            parser = config.getComponent(COMPONENT_KEY);
        }
        return parser;
    }

    /**
     * Gets this Rfc5424Layout's content format. Specified by:
     * <ul>
     * <li>Key: "structured" Value: "true"</li>
     * <li>Key: "format" Value: "RFC5424"</li>
     * </ul>
     *
     * @return Map of content format keys supporting Rfc5424Layout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("structured", "true");
        result.put("formatType", "RFC5424");
        return result;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the RFC 5424 Syslog specification.
     *
     * @param event The LogEvent.
     * @return The RFC 5424 String representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = getStringBuilder();
        appendPriority(buf, event.getLevel());
        appendTimestamp(buf, event.getTimeMillis());
        appendSpace(buf);
        appendHostName(buf);
        appendSpace(buf);
        appendAppName(buf);
        appendSpace(buf);
        appendProcessId(buf);
        appendSpace(buf);
        appendMessageId(buf, event.getMessage());
        appendSpace(buf);
        appendStructuredElements(buf, event);
        appendMessage(buf, event);
        if (useTlsMessageFormat) {
            return new TlsSyslogFrame(buf.toString()).toString();
        }
        return buf.toString();
    }

    private void appendPriority(final StringBuilder buffer, final Level logLevel) {
        buffer.append('<');
        buffer.append(Priority.getPriority(facility, logLevel));
        buffer.append(">1 ");
    }

    private void appendTimestamp(final StringBuilder buffer, final long milliseconds) {
        buffer.append(computeTimeStampString(milliseconds));
    }

    private void appendSpace(final StringBuilder buffer) {
        buffer.append(' ');
    }

    private void appendHostName(final StringBuilder buffer) {
        buffer.append(localHostName);
    }

    private void appendAppName(final StringBuilder buffer) {
        if (appName != null) {
            buffer.append(appName);
        } else if (configName != null) {
            buffer.append(configName);
        } else {
            buffer.append('-');
        }
    }

    private void appendProcessId(final StringBuilder buffer) {
        buffer.append(getProcId());
    }

    private void appendMessageId(final StringBuilder buffer, final Message message) {
        final boolean isStructured = message instanceof StructuredDataMessage;
        final String type = isStructured ? ((StructuredDataMessage) message).getType() : null;
        if (type != null) {
            buffer.append(type);
        } else if (messageId != null) {
            buffer.append(messageId);
        } else {
            buffer.append('-');
        }
    }

    private void appendMessage(final StringBuilder buffer, final LogEvent event) {
        final Message message = event.getMessage();
        // This layout formats StructuredDataMessages instead of delegating to the Message itself.
        final String text = (message instanceof StructuredDataMessage || message instanceof MessageCollectionMessage)
                ? message.getFormat()
                : message.getFormattedMessage();

        if (text != null && text.length() > 0) {
            buffer.append(' ').append(escapeNewlines(text, escapeNewLine));
        }

        if (exceptionFormatters != null && event.getThrown() != null) {
            final StringBuilder exception = new StringBuilder(LF);
            for (final PatternFormatter formatter : exceptionFormatters) {
                formatter.format(event, exception);
            }
            buffer.append(escapeNewlines(exception.toString(), escapeNewLine));
        }
        if (includeNewLine) {
            buffer.append(LF);
        }
    }

    private void appendStructuredElements(final StringBuilder buffer, final LogEvent event) {
        final Message message = event.getMessage();
        final boolean isStructured =
                message instanceof StructuredDataMessage || message instanceof StructuredDataCollectionMessage;

        if (!isStructured && (fieldFormatters != null && fieldFormatters.isEmpty()) && !includeMdc) {
            buffer.append('-');
            return;
        }

        final Map<String, StructuredDataElement> sdElements = new HashMap<>();
        final Map<String, String> contextMap = event.getContextData().toMap();

        if (mdcRequired != null) {
            checkRequired(contextMap);
        }

        if (fieldFormatters != null) {
            for (final Map.Entry<String, FieldFormatter> sdElement : fieldFormatters.entrySet()) {
                final String sdId = sdElement.getKey();
                final StructuredDataElement elem = sdElement.getValue().format(event);
                sdElements.put(sdId, elem);
            }
        }

        if (includeMdc && contextMap.size() > 0) {
            final String mdcSdIdStr = mdcSdId.toString();
            final StructuredDataElement union = sdElements.get(mdcSdIdStr);
            if (union != null) {
                union.union(contextMap);
                sdElements.put(mdcSdIdStr, union);
            } else {
                final StructuredDataElement formattedContextMap =
                        new StructuredDataElement(contextMap, mdcPrefix, false);
                sdElements.put(mdcSdIdStr, formattedContextMap);
            }
        }

        if (isStructured) {
            if (message instanceof MessageCollectionMessage) {
                for (final StructuredDataMessage data : ((StructuredDataCollectionMessage) message)) {
                    addStructuredData(sdElements, data);
                }
            } else {
                addStructuredData(sdElements, (StructuredDataMessage) message);
            }
        }

        if (sdElements.isEmpty()) {
            buffer.append('-');
            return;
        }

        for (final Map.Entry<String, StructuredDataElement> entry : sdElements.entrySet()) {
            formatStructuredElement(entry.getKey(), entry.getValue(), buffer, listChecker);
        }
    }

    private void addStructuredData(
            final Map<String, StructuredDataElement> sdElements, final StructuredDataMessage data) {
        final Map<String, String> map = data.getData();
        final StructuredDataId id = data.getId();
        final String sdId = getId(id);

        if (sdElements.containsKey(sdId)) {
            final StructuredDataElement union = sdElements.get(id.toString());
            union.union(map);
            sdElements.put(sdId, union);
        } else {
            final StructuredDataElement formattedData = new StructuredDataElement(map, eventPrefix, false);
            sdElements.put(sdId, formattedData);
        }
    }

    private String escapeNewlines(final String text, final String replacement) {
        if (null == replacement) {
            return text;
        }
        return NEWLINE_PATTERN.matcher(text).replaceAll(replacement);
    }

    protected String getProcId() {
        return procId;
    }

    protected List<String> getMdcExcludes() {
        return mdcExcludes;
    }

    protected List<String> getMdcIncludes() {
        return mdcIncludes;
    }

    private String computeTimeStampString(final long now) {
        long last;
        synchronized (this) {
            last = lastTimestamp;
            if (now == lastTimestamp) {
                return timestamppStr;
            }
        }

        final StringBuilder buffer = new StringBuilder();
        final Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        buffer.append(Integer.toString(cal.get(Calendar.YEAR)));
        buffer.append('-');
        pad(cal.get(Calendar.MONTH) + 1, TWO_DIGITS, buffer);
        buffer.append('-');
        pad(cal.get(Calendar.DAY_OF_MONTH), TWO_DIGITS, buffer);
        buffer.append('T');
        pad(cal.get(Calendar.HOUR_OF_DAY), TWO_DIGITS, buffer);
        buffer.append(':');
        pad(cal.get(Calendar.MINUTE), TWO_DIGITS, buffer);
        buffer.append(':');
        pad(cal.get(Calendar.SECOND), TWO_DIGITS, buffer);
        buffer.append('.');
        pad(cal.get(Calendar.MILLISECOND), THREE_DIGITS, buffer);

        int tzmin = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / MILLIS_PER_MINUTE;
        if (tzmin == 0) {
            buffer.append('Z');
        } else {
            if (tzmin < 0) {
                tzmin = -tzmin;
                buffer.append('-');
            } else {
                buffer.append('+');
            }
            final int tzhour = tzmin / MINUTES_PER_HOUR;
            tzmin -= tzhour * MINUTES_PER_HOUR;
            pad(tzhour, TWO_DIGITS, buffer);
            buffer.append(':');
            pad(tzmin, TWO_DIGITS, buffer);
        }
        synchronized (this) {
            if (last == lastTimestamp) {
                lastTimestamp = now;
                timestamppStr = buffer.toString();
            }
        }
        return buffer.toString();
    }

    private void pad(final int val, int max, final StringBuilder buf) {
        while (max > 1) {
            if (val < max) {
                buf.append('0');
            }
            max = max / TWO_DIGITS;
        }
        buf.append(Integer.toString(val));
    }

    private void formatStructuredElement(
            final String id, final StructuredDataElement data, final StringBuilder sb, final ListChecker checker) {
        if ((id == null && defaultId == null) || data.discard()) {
            return;
        }

        sb.append('[');
        sb.append(id);
        if (!mdcSdId.toString().equals(id)) {
            appendMap(data.getPrefix(), data.getFields(), sb, ListChecker.NOOP_CHECKER);
        } else {
            appendMap(data.getPrefix(), data.getFields(), sb, checker);
        }
        sb.append(']');
    }

    private String getId(final StructuredDataId id) {
        final StringBuilder sb = new StringBuilder();
        if (id == null || id.getName() == null) {
            sb.append(defaultId);
        } else {
            sb.append(id.getName());
        }
        String ein = id != null ? id.getEnterpriseNumber() : enterpriseNumber;
        if (StructuredDataId.RESERVED.equals(ein)) {
            ein = enterpriseNumber;
        }
        if (!StructuredDataId.RESERVED.equals(ein)) {
            sb.append('@').append(ein);
        }
        return sb.toString();
    }

    private void checkRequired(final Map<String, String> map) {
        for (final String key : mdcRequired) {
            final String value = map.get(key);
            if (value == null) {
                throw new LoggingException("Required key " + key + " is missing from the " + mdcId);
            }
        }
    }

    private void appendMap(
            final String prefix, final Map<String, String> map, final StringBuilder sb, final ListChecker checker) {
        final SortedMap<String, String> sorted = new TreeMap<>(map);
        for (final Map.Entry<String, String> entry : sorted.entrySet()) {
            if (checker.check(entry.getKey()) && entry.getValue() != null) {
                sb.append(' ');
                if (prefix != null) {
                    sb.append(prefix);
                }
                final String safeKey = escapeNewlines(escapeSDParams(entry.getKey()), escapeNewLine);
                final String safeValue = escapeNewlines(escapeSDParams(entry.getValue()), escapeNewLine);
                StringBuilders.appendKeyDqValue(sb, safeKey, safeValue);
            }
        }
    }

    private String escapeSDParams(final String value) {
        StringBuilder output = null;
        for (int i = 0; i < value.length(); i++) {
            final char cur = value.charAt(i);
            if (cur == '"' || cur == ']' || cur == '\\') {
                if (output == null) {
                    output = new StringBuilder(value.substring(0, i));
                }
                output.append("\\");
            }
            if (output != null) {
                output.append(cur);
            }
        }
        return output != null ? output.toString() : value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("facility=").append(facility.name());
        sb.append(" appName=").append(appName);
        sb.append(" defaultId=").append(defaultId);
        sb.append(" enterpriseNumber=").append(enterpriseNumber);
        sb.append(" newLine=").append(includeNewLine);
        sb.append(" includeMDC=").append(includeMdc);
        sb.append(" messageId=").append(messageId);
        return sb.toString();
    }

    /**
     * Create the RFC 5424 Layout.
     *
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param enterpriseNumber The IANA enterprise number.
     * @param includeMDC Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     *            record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param newLine If true, a newline will be appended to the end of the syslog record. The default is false.
     * @param escapeNL String that should be used to replace newlines within the message text.
     * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
     * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
     * @param excludes A comma separated list of MDC keys that should be excluded from the LogEvent.
     * @param includes A comma separated list of MDC keys that should be included in the FlumeEvent.
     * @param required A comma separated list of MDC keys that must be present in the MDC.
     * @param exceptionPattern The pattern for formatting exceptions.
     * @param useTlsMessageFormat If true the message will be formatted according to RFC 5425.
     * @param loggerFields Container for the KeyValuePairs containing the patterns
     * @param config The Configuration. Some Converters require access to the Interpolator.
     * @return An Rfc5424Layout.
     * @deprecated Use {@link Rfc5424LayoutBuilder instead}
     */
    @PluginFactory
    public static Rfc5424Layout createLayout(
            // @formatter:off
            @PluginAttribute(value = "facility", defaultString = "LOCAL0") final Facility facility,
            @PluginAttribute("id") final String id,
            @PluginAttribute(value = "enterpriseNumber", defaultInt = DEFAULT_ENTERPRISE_NUMBER)
                    final int enterpriseNumber,
            @PluginAttribute(value = "includeMDC", defaultBoolean = true) final boolean includeMDC,
            @PluginAttribute(value = "mdcId", defaultString = DEFAULT_MDCID) final String mdcId,
            @PluginAttribute("mdcPrefix") final String mdcPrefix,
            @PluginAttribute("eventPrefix") final String eventPrefix,
            @PluginAttribute(value = "newLine") final boolean newLine,
            @PluginAttribute("newLineEscape") final String escapeNL,
            @PluginAttribute("appName") final String appName,
            @PluginAttribute("messageId") final String msgId,
            @PluginAttribute("mdcExcludes") final String excludes,
            @PluginAttribute("mdcIncludes") String includes,
            @PluginAttribute("mdcRequired") final String required,
            @PluginAttribute("exceptionPattern") final String exceptionPattern,
            // RFC 5425
            @PluginAttribute(value = "useTlsMessageFormat") final boolean useTlsMessageFormat,
            @PluginElement("LoggerFields") final LoggerFields[] loggerFields,
            @PluginConfiguration final Configuration config) {
        // @formatter:on
        if (includes != null && excludes != null) {
            LOGGER.error("mdcIncludes and mdcExcludes are mutually exclusive. Includes wil be ignored");
            includes = null;
        }

        return new Rfc5424Layout(
                config,
                facility,
                id,
                String.valueOf(enterpriseNumber),
                includeMDC,
                newLine,
                escapeNL,
                mdcId,
                mdcPrefix,
                eventPrefix,
                appName,
                msgId,
                excludes,
                includes,
                required,
                StandardCharsets.UTF_8,
                exceptionPattern,
                useTlsMessageFormat,
                loggerFields);
    }

    public static class Rfc5424LayoutBuilder {
        private Configuration config;
        private Facility facility = Facility.LOCAL0;
        private String id;
        private String ein = String.valueOf(DEFAULT_ENTERPRISE_NUMBER);
        private boolean includeMDC = true;
        private boolean includeNL;
        private String escapeNL;
        private String mdcId = DEFAULT_MDCID;
        private String mdcPrefix;
        private String eventPrefix;
        private String appName;
        private String messageId;
        private String excludes;
        private String includes;
        private String required;
        private Charset charset;
        private String exceptionPattern;
        private boolean useTLSMessageFormat;
        private LoggerFields[] loggerFields;

        public Rfc5424LayoutBuilder setConfig(final Configuration config) {
            this.config = config;
            return this;
        }

        public Rfc5424LayoutBuilder setFacility(final Facility facility) {
            this.facility = facility;
            return this;
        }

        public Rfc5424LayoutBuilder setId(final String id) {
            this.id = id;
            return this;
        }

        public Rfc5424LayoutBuilder setEin(final String ein) {
            this.ein = ein;
            return this;
        }

        public Rfc5424LayoutBuilder setIncludeMDC(final boolean includeMDC) {
            this.includeMDC = includeMDC;
            return this;
        }

        public Rfc5424LayoutBuilder setIncludeNL(final boolean includeNL) {
            this.includeNL = includeNL;
            return this;
        }

        public Rfc5424LayoutBuilder setEscapeNL(final String escapeNL) {
            this.escapeNL = escapeNL;
            return this;
        }

        public Rfc5424LayoutBuilder setMdcId(final String mdcId) {
            this.mdcId = mdcId;
            return this;
        }

        public Rfc5424LayoutBuilder setMdcPrefix(final String mdcPrefix) {
            this.mdcPrefix = mdcPrefix;
            return this;
        }

        public Rfc5424LayoutBuilder setEventPrefix(final String eventPrefix) {
            this.eventPrefix = eventPrefix;
            return this;
        }

        public Rfc5424LayoutBuilder setAppName(final String appName) {
            this.appName = appName;
            return this;
        }

        public Rfc5424LayoutBuilder setMessageId(final String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Rfc5424LayoutBuilder setExcludes(final String excludes) {
            this.excludes = excludes;
            return this;
        }

        public Rfc5424LayoutBuilder setIncludes(String includes) {
            this.includes = includes;
            return this;
        }

        public Rfc5424LayoutBuilder setRequired(final String required) {
            this.required = required;
            return this;
        }

        public Rfc5424LayoutBuilder setCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public Rfc5424LayoutBuilder setExceptionPattern(final String exceptionPattern) {
            this.exceptionPattern = exceptionPattern;
            return this;
        }

        public Rfc5424LayoutBuilder setUseTLSMessageFormat(final boolean useTLSMessageFormat) {
            this.useTLSMessageFormat = useTLSMessageFormat;
            return this;
        }

        public Rfc5424LayoutBuilder setLoggerFields(final LoggerFields[] loggerFields) {
            this.loggerFields = loggerFields;
            return this;
        }

        public Rfc5424Layout build() {
            if (includes != null && excludes != null) {
                LOGGER.error("mdcIncludes and mdcExcludes are mutually exclusive. Includes wil be ignored");
                includes = null;
            }

            if (ein != null && !ENTERPRISE_ID_PATTERN.matcher(ein).matches()) {
                LOGGER.warn(String.format("provided EID %s is not in valid format!", ein));
                return null;
            }

            return new Rfc5424Layout(
                    config,
                    facility,
                    id,
                    ein,
                    includeMDC,
                    includeNL,
                    escapeNL,
                    mdcId,
                    mdcPrefix,
                    eventPrefix,
                    appName,
                    messageId,
                    excludes,
                    includes,
                    required,
                    charset,
                    exceptionPattern,
                    useTLSMessageFormat,
                    loggerFields);
        }
    }

    private class FieldFormatter {

        private final Map<String, List<PatternFormatter>> delegateMap;
        private final boolean discardIfEmpty;

        public FieldFormatter(final Map<String, List<PatternFormatter>> fieldMap, final boolean discardIfEmpty) {
            this.discardIfEmpty = discardIfEmpty;
            this.delegateMap = fieldMap;
        }

        public StructuredDataElement format(final LogEvent event) {
            final Map<String, String> map = new HashMap<>(delegateMap.size());

            for (final Map.Entry<String, List<PatternFormatter>> entry : delegateMap.entrySet()) {
                final StringBuilder buffer = new StringBuilder();
                for (final PatternFormatter formatter : entry.getValue()) {
                    formatter.format(event, buffer);
                }
                map.put(entry.getKey(), buffer.toString());
            }
            return new StructuredDataElement(map, eventPrefix, discardIfEmpty);
        }
    }

    private class StructuredDataElement {

        private final Map<String, String> fields;
        private final boolean discardIfEmpty;
        private final String prefix;

        public StructuredDataElement(
                final Map<String, String> fields, final String prefix, final boolean discardIfEmpty) {
            this.discardIfEmpty = discardIfEmpty;
            this.fields = fields;
            this.prefix = prefix;
        }

        boolean discard() {
            if (discardIfEmpty == false) {
                return false;
            }
            boolean foundNotEmptyValue = false;
            for (final Map.Entry<String, String> entry : fields.entrySet()) {
                if (Strings.isNotEmpty(entry.getValue())) {
                    foundNotEmptyValue = true;
                    break;
                }
            }
            return !foundNotEmptyValue;
        }

        void union(final Map<String, String> addFields) {
            this.fields.putAll(addFields);
        }

        Map<String, String> getFields() {
            return this.fields;
        }

        String getPrefix() {
            return prefix;
        }
    }

    public Facility getFacility() {
        return facility;
    }

    public String getDefaultId() {
        return defaultId;
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
}

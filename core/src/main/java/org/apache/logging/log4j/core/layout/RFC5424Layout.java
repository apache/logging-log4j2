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

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.NetUtils;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Formats a log event in accordance with RFC 5424.
 */
@Plugin(name = "RFC5424Layout", type = "Core", elementType = "layout", printObject = true)
public final class RFC5424Layout extends AbstractStringLayout {

    /**
     * Not a very good default - it is the Apache Software Foundation's enterprise number.
     */
    public static final int DEFAULT_ENTERPRISE_NUMBER = 18060;
    /**
     * The default event id.
     */
    public static final String DEFAULT_ID = "Audit";
    /**
     * Match newlines in a platform-independent manner.
     */
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    private static final String DEFAULT_MDCID = "mdc";
    private static final int TWO_DIGITS = 10;
    private static final int THREE_DIGITS = 100;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int MINUTES_PER_HOUR = 60;

    private static final String COMPONENT_KEY = "RFC5424-Converter";

    private final Facility facility;
    private final String defaultId;
    private final Integer enterpriseNumber;
    private final boolean includeMDC;
    private final String mdcId;
    private final String localHostName;
    private final String appName;
    private final String messageId;
    private final String configName;
    private final List<String> mdcExcludes;
    private final List<String> mdcIncludes;
    private final List<String> mdcRequired;
    private final ListChecker checker;
    private final ListChecker noopChecker = new NoopChecker();
    private final boolean includeNewLine;
    private final String escapeNewLine;

    private long lastTimestamp = -1;
    private String timestamppStr;

    private final List<PatternFormatter> formatters;

    private RFC5424Layout(final Configuration config, final Facility facility, final String id, final int ein,
                          final boolean includeMDC, final boolean includeNL, final String escapeNL, final String mdcId,
                          final String appName, final String messageId, final String excludes, final String includes,
                          final String required, final Charset charset, final String exceptionPattern) {
        super(charset);
        final PatternParser parser = createPatternParser(config);
        formatters = exceptionPattern == null ? null : parser.parse(exceptionPattern, false);
        this.facility = facility;
        this.defaultId = id == null ? DEFAULT_ID : id;
        this.enterpriseNumber = ein;
        this.includeMDC = includeMDC;
        this.includeNewLine = includeNL;
        this.escapeNewLine = escapeNL == null ? null : Matcher.quoteReplacement(escapeNL);
        this.mdcId = mdcId;
        this.appName = appName;
        this.messageId = messageId;
        this.localHostName = NetUtils.getLocalHostname();
        ListChecker c = null;
        if (excludes != null) {
            final String[] array = excludes.split(",");
            if (array.length > 0) {
                c = new ExcludeChecker();
                mdcExcludes = new ArrayList<String>(array.length);
                for (final String str : array) {
                    mdcExcludes.add(str.trim());
                }
            } else {
                mdcExcludes = null;
            }
        } else {
            mdcExcludes = null;
        }
        if (includes != null) {
            final String[] array = includes.split(",");
            if (array.length > 0) {
                c = new IncludeChecker();
                mdcIncludes = new ArrayList<String>(array.length);
                for (final String str : array) {
                    mdcIncludes.add(str.trim());
                }
            } else {
                mdcIncludes = null;
            }
        } else {
            mdcIncludes = null;
        }
        if (required != null) {
            final String[] array = required.split(",");
            if (array.length > 0) {
                mdcRequired = new ArrayList<String>(array.length);
                for (final String str : array) {
                    mdcRequired.add(str.trim());
                }
            } else {
                mdcRequired = null;
            }

        } else {
            mdcRequired = null;
        }
        this.checker = c != null ? c : noopChecker;
        final String name = config == null ? null : config.getName();
        configName = name != null && name.length() > 0 ? name : null;
    }

    /**
     * Create a PatternParser.
     * @param config The Configuration.
     * @return The PatternParser.
     */
    public static PatternParser createPatternParser(final Configuration config) {
        if (config == null) {
            return new PatternParser(config, PatternLayout.KEY, LogEventPatternConverter.class,
                ThrowablePatternConverter.class);
        }
        PatternParser parser = (PatternParser) config.getComponent(COMPONENT_KEY);
        if (parser == null) {
            parser = new PatternParser(config, PatternLayout.KEY, ThrowablePatternConverter.class);
            config.addComponent(COMPONENT_KEY, parser);
            parser = (PatternParser) config.getComponent(COMPONENT_KEY);
        }
        return parser;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the RFC 5424 Syslog specification.
     *
     * @param event The LogEvent.
     * @return The RFC 5424 String representation of the LogEvent.
     */
    public String toSerializable(final LogEvent event) {
        final Message msg = event.getMessage();
        final boolean isStructured = msg instanceof StructuredDataMessage;
        final StringBuilder buf = new StringBuilder();

        buf.append("<");
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append(">1 ");
        buf.append(computeTimeStampString(event.getMillis()));
        buf.append(' ');
        buf.append(localHostName);
        buf.append(' ');
        if (appName != null) {
            buf.append(appName);
        } else if (configName != null) {
            buf.append(configName);
        } else {
            buf.append("-");
        }
        buf.append(" ");
        buf.append(getProcId());
        buf.append(" ");
        final String type = isStructured ? ((StructuredDataMessage) msg).getType() : null;
        if (type != null) {
            buf.append(type);
        } else if (messageId != null) {
            buf.append(messageId);
        } else {
            buf.append("-");
        }
        buf.append(" ");
        if (isStructured || includeMDC) {
            StructuredDataId id = null;
            String text;
            if (isStructured) {
                final StructuredDataMessage data = (StructuredDataMessage) msg;
                final Map<String, String> map = data.getData();
                id = data.getId();
                formatStructuredElement(id, map, buf, noopChecker);
                text = data.getFormat();
            } else {
                text = msg.getFormattedMessage();
            }
            if (includeMDC) {
                if (mdcRequired != null) {
                    checkRequired(event.getContextMap());
                }
                final int ein = id == null || id.getEnterpriseNumber() < 0 ?
                    enterpriseNumber : id.getEnterpriseNumber();
                final StructuredDataId mdcSDID = new StructuredDataId(mdcId, ein, null, null);
                formatStructuredElement(mdcSDID, event.getContextMap(), buf, checker);
            }
            if (text != null && text.length() > 0) {
                buf.append(" ").append(escapeNewlines(text, escapeNewLine));
            }
        } else {
            buf.append("- ");
            buf.append(escapeNewlines(msg.getFormattedMessage(), escapeNewLine));
        }
        if (formatters != null && event.getThrown() != null) {
            final StringBuilder exception = new StringBuilder("\n");
            for (final PatternFormatter formatter : formatters) {
                formatter.format(event, exception);
            }
            buf.append(escapeNewlines(exception.toString(), escapeNewLine));
        }
        if (includeNewLine) {
            buf.append("\n");
        }
        return buf.toString();
    }

    private String escapeNewlines(final String text, final String escapeNewLine)
    {
        if (null == escapeNewLine) {
            return text;
        }
        return NEWLINE_PATTERN.matcher(text).replaceAll(escapeNewLine);
    }

    protected String getProcId() {
        return "-";
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

        final StringBuilder buf = new StringBuilder();
        final Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        buf.append(Integer.toString(cal.get(Calendar.YEAR)));
        buf.append("-");
        pad(cal.get(Calendar.MONTH) + 1, TWO_DIGITS, buf);
        buf.append("-");
        pad(cal.get(Calendar.DAY_OF_MONTH), TWO_DIGITS, buf);
        buf.append("T");
        pad(cal.get(Calendar.HOUR_OF_DAY), TWO_DIGITS, buf);
        buf.append(":");
        pad(cal.get(Calendar.MINUTE), TWO_DIGITS, buf);
        buf.append(":");
        pad(cal.get(Calendar.SECOND), TWO_DIGITS, buf);

        final int millis = cal.get(Calendar.MILLISECOND);
        if (millis != 0) {
            buf.append('.');
            pad(millis, THREE_DIGITS, buf);
        }

        int tzmin = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / MILLIS_PER_MINUTE;
        if (tzmin == 0) {
            buf.append("Z");
        } else {
            if (tzmin < 0) {
                tzmin = -tzmin;
                buf.append("-");
            } else {
                buf.append("+");
            }
            final int tzhour = tzmin / MINUTES_PER_HOUR;
            tzmin -= tzhour * MINUTES_PER_HOUR;
            pad(tzhour, TWO_DIGITS, buf);
            buf.append(":");
            pad(tzmin, TWO_DIGITS, buf);
        }
        synchronized (this) {
            if (last == lastTimestamp) {
                lastTimestamp = now;
                timestamppStr = buf.toString();
            }
        }
        return buf.toString();
    }

    private void pad(final int val, int max, final StringBuilder buf) {
        while (max > 1) {
            if (val < max) {
                buf.append("0");
            }
            max = max / TWO_DIGITS;
        }
        buf.append(Integer.toString(val));
    }

    private void formatStructuredElement(final StructuredDataId id, final Map<String, String> data,
                                         final StringBuilder sb, final ListChecker checker) {
        if (id == null && defaultId == null) {
            return;
        }
        sb.append("[");
        sb.append(getId(id));
        appendMap(data, sb, checker);
        sb.append("]");
    }

    private String getId(final StructuredDataId id) {
        final StringBuilder sb = new StringBuilder();
        if (id.getName() == null) {
            sb.append(defaultId);
        } else {
            sb.append(id.getName());
        }
        int ein = id.getEnterpriseNumber();
        if (ein < 0) {
            ein = enterpriseNumber;
        }
        if (ein >= 0) {
            sb.append("@").append(ein);
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

    private void appendMap(final Map<String, String> map, final StringBuilder sb, final ListChecker checker)
    {
        final SortedMap<String, String> sorted = new TreeMap<String, String>(map);
        for (final Map.Entry<String, String> entry : sorted.entrySet()) {
            if (checker.check(entry.getKey()) && entry.getValue() != null) {
                sb.append(" ");
                sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
    }

    /**
     * Interface used to check keys in a Map.
     */
    private interface ListChecker {
        boolean check(String key);
    }

    /**
     * Includes only the listed keys.
     */
    private class IncludeChecker implements ListChecker {
        public boolean check(final String key) {
            return mdcIncludes.contains(key);
        }
    }

    /**
     * Excludes the listed keys.
     */
    private class ExcludeChecker implements ListChecker {
        public boolean check(final String key) {
            return !mdcExcludes.contains(key);
        }
    }

    /**
     * Does nothing.
     */
    private class NoopChecker implements ListChecker {
        public boolean check(final String key) {
            return true;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("facility=").append(facility.name());
        sb.append(" appName=").append(appName);
        sb.append(" defaultId=").append(defaultId);
        sb.append(" enterpriseNumber=").append(enterpriseNumber);
        sb.append(" newLine=").append(includeNewLine);
        sb.append(" includeMDC=").append(includeMDC);
        sb.append(" messageId=").append(messageId);
        return sb.toString();
    }

    /**
     * Create the RFC 5424 Layout.
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param ein The IANA enterprise number.
     * @param includeMDC Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     * record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param includeNL If true, a newline will be appended to the end of the syslog record. The default is false.
     * @param escapeNL String that should be used to replace newlines within the message text.
     * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
     * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
     * @param excludes A comma separated list of mdc keys that should be excluded from the LogEvent.
     * @param includes A comma separated list of mdc keys that should be included in the FlumeEvent.
     * @param required A comma separated list of mdc keys that must be present in the MDC.
     * @param charsetName The character set.
     * @param exceptionPattern The pattern for formatting exceptions.
     * @param config The Configuration. Some Converters require access to the Interpolator.
     * @return An RFC5424Layout.
     */
    @PluginFactory
    public static RFC5424Layout createLayout(@PluginAttr("facility") final String facility,
                                             @PluginAttr("id") final String id,
                                             @PluginAttr("enterpriseNumber") final String ein,
                                             @PluginAttr("includeMDC") final String includeMDC,
                                             @PluginAttr("mdcId") String mdcId,
                                             @PluginAttr("newLine") final String includeNL,
                                             @PluginAttr("newLineEscape") final String escapeNL,
                                             @PluginAttr("appName") final String appName,
                                             @PluginAttr("messageId") final String msgId,
                                             @PluginAttr("mdcExcludes") final String excludes,
                                             @PluginAttr("mdcIncludes") String includes,
                                             @PluginAttr("mdcRequired") final String required,
                                             @PluginAttr("charset") final String charsetName,
                                             @PluginAttr("exceptionPattern") final String exceptionPattern,
                                             @PluginConfiguration final Configuration config) {
        final Charset charset = Charsets.getSupportedCharset(charsetName);
        if (includes != null && excludes != null) {
            LOGGER.error("mdcIncludes and mdcExcludes are mutually exclusive. Includes wil be ignored");
            includes = null;
        }
        final Facility f = Facility.toFacility(facility, Facility.LOCAL0);
        final int enterpriseNumber = ein == null ? DEFAULT_ENTERPRISE_NUMBER : Integer.parseInt(ein);
        final boolean isMdc = includeMDC == null ? true : Boolean.valueOf(includeMDC);
        final boolean includeNewLine = includeNL == null ? false : Boolean.valueOf(includeNL);
        if (mdcId == null) {
            mdcId = DEFAULT_MDCID;
        }

        return new RFC5424Layout(config, f, id, enterpriseNumber, isMdc, includeNewLine, escapeNL, mdcId, appName,
                                 msgId, excludes, includes, required, charset, exceptionPattern);
    }
}

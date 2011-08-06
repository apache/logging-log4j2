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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 *
 */
@Plugin(name="RFC5424Layout",type="Core",elementType="layout",printObject=true)
public class RFC5424Layout extends AbstractStringLayout {

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

    private static final String DEFAULT_MDCID = "mdc";

    private long lastTimestamp = -1;
    private String timestamppStr = null;

    private static final DecimalFormat TWO_DIGIT = new DecimalFormat("00");
    private static final DecimalFormat FOUR_DIGIT = new DecimalFormat("0000");

    // Not a very good default - it is the Apache Software Foundation's.
    public static final int DEFAULT_ENTERPRISE_NUMBER = 18060;
    public static final String DEFAULT_ID = "Audit";

    public RFC5424Layout(Facility facility, String id, int ein, boolean includeMDC, boolean includeNL, String mdcId,
                         String appName, String messageId, String excludes, String includes, String required,
                         Charset charset) {
        super(charset);
        this.facility = facility;
        this.defaultId = id == null ? DEFAULT_ID : id;
        this.enterpriseNumber = ein;
        this.includeMDC = includeMDC;
        this.includeNewLine = includeNL;
        this.mdcId = mdcId;
        this.appName = appName;
        this.messageId = messageId;
        this.localHostName = getLocalHostname();
        ListChecker c = null;
        if (excludes != null) {
            String[] array = excludes.split(",");
            if (array.length > 0) {
                c = new ExcludeChecker();
                mdcExcludes = new ArrayList<String>(array.length);
                for (String str : array) {
                    mdcExcludes.add(str.trim());
                }
            } else {
                mdcExcludes = null;
            }
        } else {
            mdcExcludes = null;
        }
        if (includes != null) {
            String[] array = includes.split(",");
            if (array.length > 0) {
                c = new IncludeChecker();
                mdcIncludes = new ArrayList<String>(array.length);
                for (String str : array) {
                    mdcIncludes.add(str.trim());
                }
            } else {
                mdcIncludes = null;
            }
        } else {
            mdcIncludes = null;
        }
        if (required != null) {
            String[] array = required.split(",");
            if (array.length > 0) {
                mdcRequired = new ArrayList<String>(array.length);
                for (String str : array) {
                    mdcRequired.add(str.trim());
                }
            } else {
                mdcRequired = null;
            }

        } else {
            mdcRequired = null;
        }
        this.checker = c != null ? c : noopChecker;
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        String name = ctx.getConfiguration().getName();
        configName = (name != null && name.length() > 0) ? name : null;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the RFC 5424 Syslog specification.
     */
    public String formatAs(final LogEvent event) {
        Message msg = event.getMessage();
        boolean isStructured = msg instanceof StructuredDataMessage;
        StringBuilder buf = new StringBuilder();

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
        String type = isStructured ? ((StructuredDataMessage) msg).getType() : null;
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
            String text = "";
            if (isStructured) {
                StructuredDataMessage data = (StructuredDataMessage) msg;
                Map map = data.getData();
                id = data.getId();
                formatStructuredElement(id, map, buf, noopChecker);
                text = data.getMessageFormat();
            } else {
                text = msg.getFormattedMessage();
            }
            if (includeMDC)
            {
                if (mdcRequired != null) {
                    checkRequired(event.getContextMap());
                }
                int ein = id == null || id.getEnterpriseNumber() < 0 ? enterpriseNumber : id.getEnterpriseNumber();
                StructuredDataId mdcSDID = new StructuredDataId(mdcId, ein, null, null);
                formatStructuredElement(mdcSDID, event.getContextMap(), buf, checker);
            }
            if (text != null && text.length() > 0) {
                buf.append(" ").append(text);
            }
        } else {
            buf.append("- ");
            buf.append(msg.getFormattedMessage());
        }
        if (includeNewLine) {
            buf.append("\n");
        }
        return buf.toString();
    }

    protected String getProcId() {
        return "-";
    }

    /**
     * This method gets the network name of the machine we are running on.
     * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
     * cannot be found.
     *
     * @return String the name of the local host
     */
    public String getLocalHostname() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException uhe) {
            logger.error("Could not determine local host name", uhe);
            return "UNKNOWN_LOCALHOST";
        }
    }

    public List<String> getMdcExcludes() {
        return mdcExcludes;
    }

    public List<String> getMdcIncludes() {
        return mdcIncludes;
    }

    private String computeTimeStampString(long now) {
        long last;
        synchronized (this) {
            last = lastTimestamp;
            if (now == lastTimestamp) {
                return timestamppStr;
            }
        }

        StringBuilder buf = new StringBuilder();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        buf.append(Integer.toString(cal.get(Calendar.YEAR)));
        buf.append("-");
        pad(cal.get(Calendar.MONTH) + 1, 10, buf);
        buf.append("-");
        pad(cal.get(Calendar.DAY_OF_MONTH), 10, buf);
        buf.append("T");
        pad(cal.get(Calendar.HOUR_OF_DAY), 10, buf);
        buf.append(":");
        pad(cal.get(Calendar.MINUTE), 10, buf);
        buf.append(":");
        pad(cal.get(Calendar.SECOND), 10, buf);

        int millis = cal.get(Calendar.MILLISECOND);
        if (millis != 0) {
            buf.append(".");
            pad((int) ((float) millis / 10F), 100, buf);
        }

        int tzmin = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        if (tzmin == 0) {
            buf.append("Z");
        } else {
            if (tzmin < 0) {
                tzmin = -tzmin;
                buf.append("-");
            } else {
                buf.append("+");
            }
            int tzhour = tzmin / 60;
            tzmin -= tzhour * 60;
            pad(tzhour, 10, buf);
            buf.append(":");
            pad(tzmin, 10, buf);
        }
        synchronized (this) {
            if (last == lastTimestamp) {
                lastTimestamp = now;
                timestamppStr = buf.toString();
            }
        }
        return buf.toString();
    }

    private void pad(int val, int max, StringBuilder buf) {
        while (max > 1) {
            if (val < max) {
                buf.append("0");
            }
            max = max / 10;
        }
        buf.append(Integer.toString(val));
    }

    private void formatStructuredElement(StructuredDataId id, Map<String, Object> data, StringBuilder sb,
                                         ListChecker checker)
    {
        if (id == null && defaultId == null)
        {
            return;
        }
        sb.append("[");
        sb.append(getId(id));
        appendMap(data, sb, checker);
        sb.append("]");
    }

    private String getId(StructuredDataId id) {
        StringBuilder sb = new StringBuilder();
        if (id.getName() == null) {
            sb.append(defaultId);
        } else {
            sb.append(id.getName());
        }
        int ein = id.getEnterpriseNumber();
        if (ein < 0) {
            ein = enterpriseNumber;
        }
        if (ein >=0) {
            sb.append("@").append(ein);
        }
        return sb.toString();
    }

    private void checkRequired(Map<String, Object> map) {
        for (String key : mdcRequired) {
            Object value = map.get(key);
            if (value == null) {
                throw new LoggingException("Required key " + key + " is missing from the " + mdcId);
            }
        }
    }

    private void appendMap(Map<String, Object> map, StringBuilder sb, ListChecker checker)
    {
        SortedMap<String, Object> sorted = new TreeMap<String, Object>(map);
        for (Map.Entry<String, Object> entry : sorted.entrySet())
        {
            if (checker.check(entry.getKey())) {
                sb.append(" ");
                sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
    }

    private interface ListChecker {
        boolean check(String key);
    }

    private class IncludeChecker implements ListChecker {
        public boolean check(String key) {
            return mdcIncludes.contains(key);
        }
    }

    private class ExcludeChecker implements ListChecker {
        public boolean check(String key) {
            return !mdcExcludes.contains(key);
        }
    }

    public class NoopChecker implements ListChecker {
        public boolean check(String key) {
            return true;
        }
    }

    @PluginFactory
    public static RFC5424Layout createLayout(@PluginAttr("facility") String facility,
                                             @PluginAttr("id") String id,
                                             @PluginAttr("enterpriseNumber") String ein,
                                             @PluginAttr("includeMDC") String includeMDC,
                                             @PluginAttr("mdcId") String mdcId,
                                             @PluginAttr("newLine") String includeNL,
                                             @PluginAttr("appName") String appName,
                                             @PluginAttr("messageId") String msgId,
                                             @PluginAttr("mdcExcludes") String excludes,
                                             @PluginAttr("mdcIncludes") String includes,
                                             @PluginAttr("mdcRequired") String required,
                                             @PluginAttr("charset") String charset) {
        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                logger.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        if (includes != null && excludes != null) {
            logger.error("mdcIncludes and mdcExcludes are mutually exclusive. Includes wil be ignored");
            includes = null;
        }
        Facility f = facility != null ? Facility.valueOf(facility.toUpperCase()) : Facility.LOCAL0;
        int enterpriseNumber = ein == null ? DEFAULT_ENTERPRISE_NUMBER : Integer.parseInt(ein);
        boolean isMdc = includeMDC == null ? true : Boolean.valueOf(includeMDC);
        boolean includeNewLine = includeNL == null ? false : Boolean.valueOf(includeNL);
        if (mdcId == null) {
            mdcId = DEFAULT_MDCID;
        }

        return new RFC5424Layout(f, id, enterpriseNumber, isMdc, includeNewLine, mdcId, appName, msgId, excludes,
                                 includes, required, c);
    }
}

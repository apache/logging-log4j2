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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Transform;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * This layout outputs events in a HTML table.
 * <p/>
 * Appenders using this layout should have their encoding set to UTF-8 or UTF-16, otherwise events containing
 * non ASCII characters could result in corrupted log files.
 */
@Plugin(name = "HTMLLayout", type = "Core", elementType = "layout", printObject = true)
public final class HTMLLayout extends AbstractStringLayout {

    private static final int BUF_SIZE = 256;

    private static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";

    private static final String LINE_SEP = System.getProperty("line.separator");

    private static final String DEFAULT_TITLE = "Log4J Log Messages";

    private static final String DEFAULT_CONTENT_TYPE = "text/html";

    private final long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

    // Print no location info by default
    private final boolean locationInfo;

    private final String title;

    private final String contentType;

    private enum FontSize {
        SMALLER("smaller"), XXSMALL("xx-small"), XSMALL("x-small"), SMALL("small"), MEDIUM("medium"), LARGE("large"),
        XLARGE("x-large"), XXLARGE("xx-large"),  LARGER("larger");

        private final String size;

        private FontSize(String size) {
            this.size = size;
        }

        public String getFontSize() {
            return size;
        }

        public static FontSize getFontSize(String size) {
            for (FontSize fontSize : values()) {
                if (fontSize.size.equals(size)) {
                    return fontSize;
                }
            }
            return SMALL;
        }

        public FontSize larger() {
            return this.ordinal() < XXLARGE.ordinal() ? FontSize.values()[this.ordinal() + 1] : this;
        }
    }

    private final String font;
    private final String fontSize;
    private final String headerSize;

    private HTMLLayout(boolean locationInfo, String title, String contentType, Charset charset,
                       String font, String fontSize, String headerSize) {
        super(charset);
        this.locationInfo = locationInfo;
        this.title = title;
        this.contentType = contentType;
        this.font = font;
        this.fontSize = fontSize;
        this.headerSize = headerSize;
    }

    /**
     * Format as a String.
     *
     * @param event The Logging Event.
     * @return A String containging the LogEvent as HTML.
     */
    public String toSerializable(LogEvent event) {
        StringBuilder sbuf = new StringBuilder(BUF_SIZE);

        sbuf.append(LINE_SEP).append("<tr>").append(LINE_SEP);

        sbuf.append("<td>");
        sbuf.append(event.getMillis() - jvmStartTime);
        sbuf.append("</td>").append(LINE_SEP);

        String escapedThread = Transform.escapeTags(event.getThreadName());
        sbuf.append("<td title=\"").append(escapedThread).append(" thread\">");
        sbuf.append(escapedThread);
        sbuf.append("</td>").append(LINE_SEP);

        sbuf.append("<td title=\"Level\">");
        if (event.getLevel().equals(Level.DEBUG)) {
            sbuf.append("<font color=\"#339933\">");
            sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
            sbuf.append("</font>");
        } else if (event.getLevel().isAtLeastAsSpecificAs(Level.WARN)) {
            sbuf.append("<font color=\"#993300\"><strong>");
            sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
            sbuf.append("</strong></font>");
        } else {
            sbuf.append(Transform.escapeTags(String.valueOf(event.getLevel())));
        }
        sbuf.append("</td>").append(LINE_SEP);

        String escapedLogger = Transform.escapeTags(event.getLoggerName());
        if (escapedLogger.length() == 0) {
            escapedLogger = "root";
        }
        sbuf.append("<td title=\"").append(escapedLogger).append(" logger\">");
        sbuf.append(escapedLogger);
        sbuf.append("</td>").append(LINE_SEP);

        if (locationInfo) {
            StackTraceElement element = event.getSource();
            sbuf.append("<td>");
            sbuf.append(Transform.escapeTags(element.getFileName()));
            sbuf.append(':');
            sbuf.append(element.getLineNumber());
            sbuf.append("</td>").append(LINE_SEP);
        }

        sbuf.append("<td title=\"Message\">");
        sbuf.append(Transform.escapeTags(event.getMessage().getFormattedMessage()).replaceAll(LINE_SEP, "<br />"));
        sbuf.append("</td>").append(LINE_SEP);
        sbuf.append("</tr>").append(LINE_SEP);

        if (event.getContextStack().getDepth() > 0) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Nested Diagnostic Context\">");
            sbuf.append("NDC: ").append(Transform.escapeTags(event.getContextStack().toString()));
            sbuf.append("</td></tr>").append(LINE_SEP);
        }

        if (event.getContextMap().size() > 0) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Mapped Diagnostic Context\">");
            sbuf.append("MDC: ").append(Transform.escapeTags(event.getContextMap().toString()));
            sbuf.append("</td></tr>").append(LINE_SEP);
        }

        Throwable throwable = event.getThrown();
        if (throwable != null) {
            sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\">");
            appendThrowableAsHTML(throwable, sbuf);
            sbuf.append("</td></tr>").append(LINE_SEP);
        }

        return sbuf.toString();
    }

    private void appendThrowableAsHTML(Throwable throwable, StringBuilder sbuf) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch (RuntimeException ex) {
            // Ignore the exception.
        }
        pw.flush();
        LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        ArrayList<String> lines = new ArrayList<String>();
        try {
          String line = reader.readLine();
          while (line != null) {
            lines.add(line);
            line = reader.readLine();
          }
        } catch (IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex.toString());
        }
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                sbuf.append(TRACE_PREFIX);
            } else {
                first = false;
            }
            sbuf.append(Transform.escapeTags(line));
            sbuf.append(LINE_SEP);
        }
    }

    /**
     * Returns appropriate HTML headers.
     * @return The header as a byte array.
     */
    @Override
    public byte[] getHeader() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" ");
        sbuf.append("\"http://www.w3.org/TR/html4/loose.dtd\">");
        sbuf.append(LINE_SEP);
        sbuf.append("<html>").append(LINE_SEP);
        sbuf.append("<head>").append(LINE_SEP);
        sbuf.append("<title>").append(title).append("</title>").append(LINE_SEP);
        sbuf.append("<style type=\"text/css\">").append(LINE_SEP);
        sbuf.append("<!--").append(LINE_SEP);
        sbuf.append("body, table {font-family:").append(font).append("; font-size: ");
        sbuf.append(headerSize).append(";}").append(LINE_SEP);
        sbuf.append("th {background: #336699; color: #FFFFFF; text-align: left;}").append(LINE_SEP);
        sbuf.append("-->").append(LINE_SEP);
        sbuf.append("</style>").append(LINE_SEP);
        sbuf.append("</head>").append(LINE_SEP);
        sbuf.append("<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">").append(LINE_SEP);
        sbuf.append("<hr size=\"1\" noshade>").append(LINE_SEP);
        sbuf.append("Log session start time " + new java.util.Date() + "<br>").append(LINE_SEP);
        sbuf.append("<br>").append(LINE_SEP);
        sbuf.append(
            "<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">");
        sbuf.append(LINE_SEP);
        sbuf.append("<tr>").append(LINE_SEP);
        sbuf.append("<th>Time</th>").append(LINE_SEP);
        sbuf.append("<th>Thread</th>").append(LINE_SEP);
        sbuf.append("<th>Level</th>").append(LINE_SEP);
        sbuf.append("<th>Logger</th>").append(LINE_SEP);
        if (locationInfo) {
            sbuf.append("<th>File:Line</th>").append(LINE_SEP);
        }
        sbuf.append("<th>Message</th>").append(LINE_SEP);
        sbuf.append("</tr>").append(LINE_SEP);
        return sbuf.toString().getBytes(getCharset());
    }

    /**
     * Returns the appropriate HTML footers.
     * @return the footer as a byet array.
     */
    @Override
    public byte[] getFooter() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("</table>").append(LINE_SEP);
        sbuf.append("<br>").append(LINE_SEP);
        sbuf.append("</body></html>");
        return sbuf.toString().getBytes(getCharset());
    }

    /**
     * Create an HTML Layout.
     * @param locationInfo If "true", location information will be included. The default is false.
     * @param title The title to include in the file header. If none is specified the default title will be used.
     * @param contentType The content type. Defaults to "text/html".
     * @param charset The character set to use. If not specified, the default will be used.
     * @return An HTML Layout.
     */
    @PluginFactory
    public static HTMLLayout createLayout(@PluginAttr("locationInfo") String locationInfo,
                                          @PluginAttr("title") String title,
                                          @PluginAttr("contentType") String contentType,
                                          @PluginAttr("charset") String charset,
                                          @PluginAttr("fontSize") String fontSize,
                                          @PluginAttr("fontName") String font) {
        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                LOGGER.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        if (font == null) {
            font = "arial,sans-serif";
        }
        FontSize fs = FontSize.getFontSize(fontSize);
        fontSize = fs.getFontSize();
        String headerSize = fs.larger().getFontSize();
        boolean info = locationInfo == null ? false : Boolean.valueOf(locationInfo);
        if (title == null) {
            title = DEFAULT_TITLE;
        }
        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        return new HTMLLayout(info, title, contentType, c, font, fontSize, headerSize);
    }
}

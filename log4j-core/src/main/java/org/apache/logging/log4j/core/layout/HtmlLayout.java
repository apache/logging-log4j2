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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Charsets;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Transform;

/**
 * Outputs events as rows in an HTML table on an HTML page.
 * <p>
 * Appenders using this layout should have their encoding set to UTF-8 or UTF-16, otherwise events containing non ASCII
 * characters could result in corrupted log files.
 * </p>
 */
@Plugin(name = "HtmlLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class HtmlLayout extends AbstractStringLayout {

    private static final long serialVersionUID = 1L;

    private static final int BUF_SIZE = 256;

    private static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";

    private static final String REGEXP = Constants.LINE_SEPARATOR.equals("\n") ? "\n" : Constants.LINE_SEPARATOR + "|\n";

    private static final String DEFAULT_TITLE = "Log4j Log Messages";

    private static final String DEFAULT_CONTENT_TYPE = "text/html";

    public static final String DEFAULT_FONT_FAMILY = "arial,sans-serif";

    private final long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

    // Print no location info by default
    private final boolean locationInfo;

    private final String title;

    private final String contentType;

    /**Possible font sizes */
    public static enum FontSize {
        SMALLER("smaller"), XXSMALL("xx-small"), XSMALL("x-small"), SMALL("small"), MEDIUM("medium"), LARGE("large"),
        XLARGE("x-large"), XXLARGE("xx-large"),  LARGER("larger");

        private final String size;

        private FontSize(final String size) {
            this.size = size;
        }

        public String getFontSize() {
            return size;
        }

        public static FontSize getFontSize(final String size) {
            for (final FontSize fontSize : values()) {
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

    private HtmlLayout(final boolean locationInfo, final String title, final String contentType, final Charset charset,
            final String font, final String fontSize, final String headerSize) {
        super(charset);
        this.locationInfo = locationInfo;
        this.title = title;
        this.contentType = addCharsetToContentType(contentType);
        this.font = font;
        this.fontSize = fontSize;
        this.headerSize = headerSize;
    }

    private String addCharsetToContentType(final String contentType) {
        if (contentType == null) {
            return DEFAULT_CONTENT_TYPE + "; charset=" + getCharset();
        }
        return contentType.contains("charset") ? contentType : contentType + "; charset=" + getCharset();
    }

    /**
     * Format as a String.
     *
     * @param event The Logging Event.
     * @return A String containing the LogEvent as HTML.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder sbuf = new StringBuilder(BUF_SIZE);

        sbuf.append(Constants.LINE_SEPARATOR).append("<tr>").append(Constants.LINE_SEPARATOR);

        sbuf.append("<td>");
        sbuf.append(event.getTimeMillis() - jvmStartTime);
        sbuf.append("</td>").append(Constants.LINE_SEPARATOR);

        final String escapedThread = Transform.escapeHtmlTags(event.getThreadName());
        sbuf.append("<td title=\"").append(escapedThread).append(" thread\">");
        sbuf.append(escapedThread);
        sbuf.append("</td>").append(Constants.LINE_SEPARATOR);

        sbuf.append("<td title=\"Level\">");
        if (event.getLevel().equals(Level.DEBUG)) {
            sbuf.append("<font color=\"#339933\">");
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
            sbuf.append("</font>");
        } else if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            sbuf.append("<font color=\"#993300\"><strong>");
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
            sbuf.append("</strong></font>");
        } else {
            sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
        }
        sbuf.append("</td>").append(Constants.LINE_SEPARATOR);

        String escapedLogger = Transform.escapeHtmlTags(event.getLoggerName());
        if (escapedLogger.isEmpty()) {
            escapedLogger = "root";
        }
        sbuf.append("<td title=\"").append(escapedLogger).append(" logger\">");
        sbuf.append(escapedLogger);
        sbuf.append("</td>").append(Constants.LINE_SEPARATOR);

        if (locationInfo) {
            final StackTraceElement element = event.getSource();
            sbuf.append("<td>");
            sbuf.append(Transform.escapeHtmlTags(element.getFileName()));
            sbuf.append(':');
            sbuf.append(element.getLineNumber());
            sbuf.append("</td>").append(Constants.LINE_SEPARATOR);
        }

        sbuf.append("<td title=\"Message\">");
        sbuf.append(Transform.escapeHtmlTags(event.getMessage().getFormattedMessage()).replaceAll(REGEXP, "<br />"));
        sbuf.append("</td>").append(Constants.LINE_SEPARATOR);
        sbuf.append("</tr>").append(Constants.LINE_SEPARATOR);

        if (event.getContextStack() != null && !event.getContextStack().isEmpty()) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Nested Diagnostic Context\">");
            sbuf.append("NDC: ").append(Transform.escapeHtmlTags(event.getContextStack().toString()));
            sbuf.append("</td></tr>").append(Constants.LINE_SEPARATOR);
        }

        if (event.getContextMap() != null && !event.getContextMap().isEmpty()) {
            sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\" ");
            sbuf.append("title=\"Mapped Diagnostic Context\">");
            sbuf.append("MDC: ").append(Transform.escapeHtmlTags(event.getContextMap().toString()));
            sbuf.append("</td></tr>").append(Constants.LINE_SEPARATOR);
        }

        final Throwable throwable = event.getThrown();
        if (throwable != null) {
            sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : ").append(fontSize);
            sbuf.append(";\" colspan=\"6\">");
            appendThrowableAsHtml(throwable, sbuf);
            sbuf.append("</td></tr>").append(Constants.LINE_SEPARATOR);
        }

        return sbuf.toString();
    }

    @Override
    /**
     * @return The content type.
     */
    public String getContentType() {
        return contentType;
    }

    private void appendThrowableAsHtml(final Throwable throwable, final StringBuilder sbuf) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch (final RuntimeException ex) {
            // Ignore the exception.
        }
        pw.flush();
        final LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        final ArrayList<String> lines = new ArrayList<String>();
        try {
          String line = reader.readLine();
          while (line != null) {
            lines.add(line);
            line = reader.readLine();
          }
        } catch (final IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex.toString());
        }
        boolean first = true;
        for (final String line : lines) {
            if (!first) {
                sbuf.append(TRACE_PREFIX);
            } else {
                first = false;
            }
            sbuf.append(Transform.escapeHtmlTags(line));
            sbuf.append(Constants.LINE_SEPARATOR);
        }
    }

    /**
     * Returns appropriate HTML headers.
     * @return The header as a byte array.
     */
    @Override
    public byte[] getHeader() {
        final StringBuilder sbuf = new StringBuilder();
        sbuf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" ");
        sbuf.append("\"http://www.w3.org/TR/html4/loose.dtd\">");
        sbuf.append(Constants.LINE_SEPARATOR);
        sbuf.append("<html>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<head>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<meta charset=\"").append(getCharset()).append("\"/>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<title>").append(title).append("</title>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<style type=\"text/css\">").append(Constants.LINE_SEPARATOR);
        sbuf.append("<!--").append(Constants.LINE_SEPARATOR);
        sbuf.append("body, table {font-family:").append(font).append("; font-size: ");
        sbuf.append(headerSize).append(";}").append(Constants.LINE_SEPARATOR);
        sbuf.append("th {background: #336699; color: #FFFFFF; text-align: left;}").append(Constants.LINE_SEPARATOR);
        sbuf.append("-->").append(Constants.LINE_SEPARATOR);
        sbuf.append("</style>").append(Constants.LINE_SEPARATOR);
        sbuf.append("</head>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">").append(Constants.LINE_SEPARATOR);
        sbuf.append("<hr size=\"1\" noshade=\"noshade\">").append(Constants.LINE_SEPARATOR);
        sbuf.append("Log session start time " + new java.util.Date() + "<br>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<br>").append(Constants.LINE_SEPARATOR);
        sbuf.append(
            "<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">");
        sbuf.append(Constants.LINE_SEPARATOR);
        sbuf.append("<tr>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<th>Time</th>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<th>Thread</th>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<th>Level</th>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<th>Logger</th>").append(Constants.LINE_SEPARATOR);
        if (locationInfo) {
            sbuf.append("<th>File:Line</th>").append(Constants.LINE_SEPARATOR);
        }
        sbuf.append("<th>Message</th>").append(Constants.LINE_SEPARATOR);
        sbuf.append("</tr>").append(Constants.LINE_SEPARATOR);
        return sbuf.toString().getBytes(getCharset());
    }

    /**
     * Returns the appropriate HTML footers.
     * @return the footer as a byet array.
     */
    @Override
    public byte[] getFooter() {
        final StringBuilder sbuf = new StringBuilder();
        sbuf.append("</table>").append(Constants.LINE_SEPARATOR);
        sbuf.append("<br>").append(Constants.LINE_SEPARATOR);
        sbuf.append("</body></html>");
        return getBytes(sbuf.toString());
    }

    /**
     * Create an HTML Layout.
     * @param locationInfo If "true", location information will be included. The default is false.
     * @param title The title to include in the file header. If none is specified the default title will be used.
     * @param contentType The content type. Defaults to "text/html".
     * @param charset The character set to use. If not specified, the default will be used.
     * @param fontSize The font size of the text.
     * @param font The font to use for the text.
     * @return An HTML Layout.
     */
    @PluginFactory
    public static HtmlLayout createLayout(
            @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
            @PluginAttribute(value = "title", defaultString = DEFAULT_TITLE) final String title,
            @PluginAttribute("contentType") String contentType,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute("fontSize") String fontSize,
            @PluginAttribute(value = "fontName", defaultString = DEFAULT_FONT_FAMILY) final String font) {
        final FontSize fs = FontSize.getFontSize(fontSize);
        fontSize = fs.getFontSize();
        final String headerSize = fs.larger().getFontSize();
        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE + "; charset=" + charset;
        }
        return new HtmlLayout(locationInfo, title, contentType, charset, font, fontSize, headerSize);
    }

    /**
     * Creates an HTML Layout using the default settings.
     *
     * @return an HTML Layout.
     */
    public static HtmlLayout createDefaultLayout() {
        return newBuilder().build();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<HtmlLayout> {

        @PluginBuilderAttribute
        private boolean locationInfo = false;

        @PluginBuilderAttribute
        private String title = DEFAULT_TITLE;

        @PluginBuilderAttribute
        private String contentType = null; // defer default value in order to use specified charset

        @PluginBuilderAttribute
        private Charset charset = Charsets.UTF_8;

        @PluginBuilderAttribute
        private FontSize fontSize = FontSize.SMALL;

        @PluginBuilderAttribute
        private String fontName = DEFAULT_FONT_FAMILY;

        private Builder() {
        }

        public Builder withLocationInfo(final boolean locationInfo) {
            this.locationInfo = locationInfo;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder withFontSize(final FontSize fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder withFontName(final String fontName) {
            this.fontName = fontName;
            return this;
        }

        @Override
        public HtmlLayout build() {
            // TODO: extract charset from content-type
            if (contentType == null) {
                contentType = DEFAULT_CONTENT_TYPE + "; charset=" + charset;
            }
            return new HtmlLayout(locationInfo, title, contentType, charset, fontName, fontSize.getFontSize(),
                fontSize.larger().getFontSize());
        }
    }
}

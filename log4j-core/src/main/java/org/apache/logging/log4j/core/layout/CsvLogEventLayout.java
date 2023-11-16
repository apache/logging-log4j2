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

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A Comma-Separated Value (CSV) layout to log events.
 *
 * Depends on Apache Commons CSV 1.2.
 *
 * @since 2.4
 */
@Plugin(name = "CsvLogEventLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class CsvLogEventLayout extends AbstractCsvLayout {

    public static CsvLogEventLayout createDefaultLayout() {
        return new CsvLogEventLayout(
                null, Charset.forName(DEFAULT_CHARSET), CSVFormat.valueOf(DEFAULT_FORMAT), null, null);
    }

    public static CsvLogEventLayout createLayout(final CSVFormat format) {
        return new CsvLogEventLayout(null, Charset.forName(DEFAULT_CHARSET), format, null, null);
    }

    @PluginFactory
    public static CsvLogEventLayout createLayout(
            // @formatter:off
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "format", defaultString = DEFAULT_FORMAT) final String format,
            @PluginAttribute("delimiter") final Character delimiter,
            @PluginAttribute("escape") final Character escape,
            @PluginAttribute("quote") final Character quote,
            @PluginAttribute("quoteMode") final QuoteMode quoteMode,
            @PluginAttribute("nullString") final String nullString,
            @PluginAttribute("recordSeparator") final String recordSeparator,
            @PluginAttribute(value = "charset", defaultString = DEFAULT_CHARSET) final Charset charset,
            @PluginAttribute("header") final String header,
            @PluginAttribute("footer") final String footer)
                // @formatter:on
            {

        final CSVFormat csvFormat =
                createFormat(format, delimiter, escape, quote, quoteMode, nullString, recordSeparator);
        return new CsvLogEventLayout(config, charset, csvFormat, header, footer);
    }

    protected CsvLogEventLayout(
            final Configuration config,
            final Charset charset,
            final CSVFormat csvFormat,
            final String header,
            final String footer) {
        super(config, charset, csvFormat, header, footer);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buffer = getStringBuilder();
        final CSVFormat format = getFormat();
        try {
            format.print(event.getNanoTime(), buffer, true);
            format.print(event.getTimeMillis(), buffer, false);
            format.print(event.getLevel(), buffer, false);
            format.print(event.getThreadId(), buffer, false);
            format.print(event.getThreadName(), buffer, false);
            format.print(event.getThreadPriority(), buffer, false);
            format.print(event.getMessage().getFormattedMessage(), buffer, false);
            format.print(event.getLoggerFqcn(), buffer, false);
            format.print(event.getLoggerName(), buffer, false);
            format.print(event.getMarker(), buffer, false);
            format.print(event.getThrownProxy(), buffer, false);
            format.print(event.getSource(), buffer, false);
            format.print(event.getContextData(), buffer, false);
            format.print(event.getContextStack(), buffer, false);
            format.println(buffer);
            return buffer.toString();
        } catch (final IOException e) {
            StatusLogger.getLogger().error(event.toString(), e);
            return format.getCommentMarker() + " " + e;
        }
    }
}

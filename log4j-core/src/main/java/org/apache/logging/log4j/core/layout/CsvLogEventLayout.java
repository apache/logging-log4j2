package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

public class CsvLogEventLayout extends CsvLayout {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static CsvLogEventLayout createDefaultLayout() {
        return new CsvLogEventLayout(Charset.forName(DEFAULT_CHARSET), CSVFormat.valueOf(DEFAULT_FORMAT), null, null);
    }

    public static CsvLogEventLayout createLayout(final CSVFormat format) {
        return new CsvLogEventLayout(Charset.forName(DEFAULT_CHARSET), format, null, null);
    }

    @PluginFactory
    public static CsvLogEventLayout createLayout(
            // @formatter:off
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

        final CSVFormat csvFormat = createFormat(format, delimiter, escape, quote, quoteMode, nullString, recordSeparator);
        return new CsvLogEventLayout(charset, csvFormat, header, footer);
    }
   
    protected CsvLogEventLayout(final Charset charset, final CSVFormat csvFormat, final String header, final String footer) {
        super(charset, csvFormat, header, footer);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buffer = new StringBuilder(1024);
        try {
            // Revisit when 1.3 is out so that we do not need to create a new
            // printer for each event.
            // No need to close the printer.
            final CSVPrinter printer = new CSVPrinter(buffer, getFormat());
            printer.print(event.getNanoTime());
            printer.print(event.getTimeMillis());
            printer.print(event.getLevel());
            printer.print(event.getThreadName());
            printer.print(event.getMessage().getFormattedMessage());
            printer.print(event.getLoggerFqcn());
            printer.print(event.getLoggerName());
            printer.print(event.getMarker());
            printer.print(event.getThrownProxy());
            printer.print(event.getSource());
            printer.print(event.getContextMap());
            printer.print(event.getContextStack());
            printer.println();
            return buffer.toString();
        } catch (final IOException e) {
            StatusLogger.getLogger().error(event.toString(), e);
            return getFormat().getCommentMarker() + " " + e;
        }
    }

}

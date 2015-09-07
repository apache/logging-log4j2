package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A Comma-Separated Value (CSV) layout to log event parameters.
 * The event message is currently ignored. 
 * 
 * <p>
 * Best used with:
 * </p>
 * <p>
 * {@code logger.debug(new ObjectArrayMessage(1, 2, "Bob"));}
 * </p>
 * 
 * Depends on Apache Commons CSV 1.2.
 * 
 * @since 2.4
 */
@Plugin(name = "CsvLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class CsvParameterLayout extends CsvLayout {

    private static final long serialVersionUID = 1L;

    public static CsvLayout createDefaultLayout() {
        return new CsvParameterLayout(Charset.forName(DEFAULT_CHARSET), CSVFormat.valueOf(DEFAULT_FORMAT), null, null);
    }

    public static CsvLayout createLayout(final CSVFormat format) {
        return new CsvParameterLayout(Charset.forName(DEFAULT_CHARSET), format, null, null);
    }

    @PluginFactory
    public static CsvLayout createLayout(
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
        return new CsvParameterLayout(charset, csvFormat, header, footer);
    }

    public CsvParameterLayout(final Charset charset, final CSVFormat csvFormat, final String header, final String footer) {
        super(charset, csvFormat, header, footer);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final Message message = event.getMessage();
        final Object[] parameters = message.getParameters();
        final StringBuilder buffer = new StringBuilder(1024);
        try {
            // Revisit when 1.3 is out so that we do not need to create a new
            // printer for each event.
            // No need to close the printer.
            final CSVPrinter printer = new CSVPrinter(buffer, getFormat());
            printer.printRecord(parameters);
            return buffer.toString();
        } catch (final IOException e) {
            StatusLogger.getLogger().error(message, e);
            return getFormat().getCommentMarker() + " " + e;
        }
    }

}

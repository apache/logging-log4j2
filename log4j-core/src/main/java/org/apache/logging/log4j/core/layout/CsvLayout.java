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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A Comma-Separated Value (CSV) layout.
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
public class CsvLayout extends AbstractStringLayout {

    private static final String CONTENT_TYPE = "text/csv";
    protected static final String DEFAULT_CHARSET = "UTF-8";
    protected static final String DEFAULT_FORMAT = "Default";
    private static final long serialVersionUID = 1L;

    protected static CSVFormat createFormat(final String format, final Character delimiter, final Character escape, final Character quote, final QuoteMode quoteMode, final String nullString,
            final String recordSeparator) {
                CSVFormat csvFormat = CSVFormat.valueOf(format);
                if (delimiter != null) {
                    csvFormat = csvFormat.withDelimiter(delimiter);
                }
                if (escape != null) {
                    csvFormat = csvFormat.withEscape(escape);
                }
                if (quote != null) {
                    csvFormat = csvFormat.withQuote(quote);
                }
                if (quoteMode != null) {
                    csvFormat = csvFormat.withQuoteMode(quoteMode);
                }
                if (nullString != null) {
                    csvFormat = csvFormat.withNullString(nullString);
                }
                if (recordSeparator != null) {
                    csvFormat = csvFormat.withRecordSeparator(recordSeparator);
                }
                return csvFormat;
            }

    private final CSVFormat format;

    protected CsvLayout(final Charset charset, final CSVFormat csvFormat, final String header,
            final String footer) {
        super(charset, toBytes(header, charset), toBytes(footer, charset));
        this.format = csvFormat;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    public CSVFormat getFormat() {
        return format;
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

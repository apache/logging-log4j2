package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

/**
 * A superclass for Comma-Separated Value (CSV) layouts.
 * 
 * Depends on Apache Commons CSV 1.2.
 * 
 * @since 2.4
 */
public abstract class AbstractCsvLayout extends AbstractStringLayout {

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

    protected AbstractCsvLayout(final Charset charset, final CSVFormat csvFormat, final String header,
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

}

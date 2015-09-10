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

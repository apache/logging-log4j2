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

import java.nio.charset.Charset;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * A superclass for Comma-Separated Value (CSV) layouts.
 *
 * Depends on Apache Commons CSV 1.2.
 *
 * @since 2.4
 */
public abstract class AbstractCsvLayout extends AbstractStringLayout {

    protected static final String DEFAULT_CHARSET = "UTF-8";
    protected static final String DEFAULT_FORMAT = "Default";
    private static final String CONTENT_TYPE = "text/csv";

    protected static CSVFormat createFormat(
            final String format,
            final Character delimiter,
            final Character escape,
            final Character quote,
            final QuoteMode quoteMode,
            final String nullString,
            final String recordSeparator) {
        CSVFormat csvFormat = CSVFormat.valueOf(format);
        if (isNotNul(delimiter)) {
            csvFormat = csvFormat.withDelimiter(delimiter);
        }
        if (isNotNul(escape)) {
            csvFormat = csvFormat.withEscape(escape);
        }
        if (isNotNul(quote)) {
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

    private static boolean isNotNul(final Character character) {
        return character != null && character.charValue() != 0;
    }

    private final CSVFormat format;

    protected AbstractCsvLayout(
            final Configuration config,
            final Charset charset,
            final CSVFormat csvFormat,
            final String header,
            final String footer) {
        super(
                config,
                charset,
                PatternLayout.newSerializerBuilder()
                        .setConfiguration(config)
                        .setPattern(header)
                        .build(),
                PatternLayout.newSerializerBuilder()
                        .setConfiguration(config)
                        .setPattern(footer)
                        .build());
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

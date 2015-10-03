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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

abstract class AbstractJacksonLayout extends AbstractStringLayout {

    protected static final String DEFAULT_EOL = "\r\n";
    protected static final String COMPACT_EOL = Strings.EMPTY;
    private static final long serialVersionUID = 1L;

    protected final String eol;
    protected final ObjectWriter objectWriter;
    protected final boolean compact;
    protected final boolean complete;

    protected AbstractJacksonLayout(final ObjectWriter objectWriter, final Charset charset, final boolean compact, final boolean complete, final boolean eventEol) {
        super(charset);
        this.objectWriter = objectWriter;
        this.compact = compact;
        this.complete = complete;
        this.eol = compact && !eventEol ? COMPACT_EOL : DEFAULT_EOL;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent}.
     * 
     * @param event The LogEvent.
     * @return The XML representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        try {
            return this.objectWriter.writeValueAsString(event) + eol;
        } catch (final JsonProcessingException e) {
            // Should this be an ISE or IAE?
            LOGGER.error(e);
            return Strings.EMPTY;
        }
    }

}

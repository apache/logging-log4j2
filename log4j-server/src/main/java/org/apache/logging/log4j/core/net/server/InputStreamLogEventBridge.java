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
package org.apache.logging.log4j.core.net.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventListener;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Reads and logs {@link LogEvent}s from an {@link InputStream}.
 */
public abstract class InputStreamLogEventBridge extends AbstractLogEventBridge<InputStream> {

    private final int bufferSize;

    private final Charset charset;

    private final String eventEndMarker;
    
    private final ObjectReader objectReader;
    
    public InputStreamLogEventBridge(final ObjectMapper mapper, final int bufferSize, final Charset charset, final String eventEndMarker) {
        this.bufferSize = bufferSize;
        this.charset = charset;
        this.eventEndMarker = eventEndMarker;
        this.objectReader = mapper.readerFor(Log4jLogEvent.class);
    }

    abstract protected int[] getEventIndices(final String text, int beginIndex);

    @Override
    public void logEvents(final InputStream inputStream, final LogEventListener logEventListener) throws IOException {
        String workingText = Strings.EMPTY;
        try {
            // Allocate buffer once
            final byte[] buffer = new byte[bufferSize];
            String textRemains = workingText = Strings.EMPTY;
            while (true) {
                // Process until the stream is EOF.
                final int streamReadLength = inputStream.read(buffer);
                if (streamReadLength == END) {
                    // The input stream is EOF
                    break;
                }
                final String text = workingText = textRemains + new String(buffer, 0, streamReadLength, charset);
                int beginIndex = 0;
                while (true) {
                    // Extract and log all XML events in the buffer
                    final int[] pair = getEventIndices(text, beginIndex);
                    final int eventStartMarkerIndex = pair[0];
                    if (eventStartMarkerIndex < 0) {
                        // No more events or partial XML only in the buffer.
                        // Save the unprocessed string part
                        textRemains = text.substring(beginIndex);
                        break;
                    }
                    final int eventEndMarkerIndex = pair[1];
                    if (eventEndMarkerIndex > 0) {
                        final int eventEndXmlIndex = eventEndMarkerIndex + eventEndMarker.length();
                        final String textEvent = workingText = text.substring(eventStartMarkerIndex, eventEndXmlIndex);
                        final LogEvent logEvent = unmarshal(textEvent);
                        logEventListener.log(logEvent);
                        beginIndex = eventEndXmlIndex;
                    } else {
                        // No more events or partial XML only in the buffer.
                        // Save the unprocessed string part
                        textRemains = text.substring(beginIndex);
                        break;
                    }
                }
            }
        } catch (final IOException ex) {
            logger.error(workingText, ex);
        }
    }

    protected Log4jLogEvent unmarshal(final String jsonEvent) throws IOException {
        return this.objectReader.readValue(jsonEvent);
    }

}

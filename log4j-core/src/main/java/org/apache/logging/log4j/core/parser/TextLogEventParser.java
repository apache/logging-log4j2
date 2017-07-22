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
package org.apache.logging.log4j.core.parser;

import org.apache.logging.log4j.core.LogEvent;

import java.io.IOException;
import java.io.Reader;

/**
 * Parse the output from a text based layout into instances of {@link LogEvent}.
 */
public interface TextLogEventParser extends LogEventParser {
    /**
     * Parse from a Reader. The reader may contain multiple log events.
     * This method will read one log event and leave the reader open and positioned to read the
     * next log event.
     *
     * @param input  the reader
     *
     * @return the parsed LogEvent, or {@literal null} of end of input is reached.
     * @throws IOException if unable to read from the input
     * @throws ParseException if the input is malformed and cannot be parsed as a LogEvent
     */
    LogEvent parseFrom(Reader input) throws IOException, ParseException;

    /**
     * Parse from a String, which is expected to contain exactly one log event.
     *
     * @param input  the string
     *
     * @return the parsed LogEvent
     * @throws ParseException if the input is malformed and cannot be parsed as a LogEvent
     */
    LogEvent parseFrom(String input) throws ParseException;
}

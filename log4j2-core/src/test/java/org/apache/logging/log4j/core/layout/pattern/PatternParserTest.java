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
package org.apache.logging.log4j.core.layout.pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.util.List;

/**
 *
 */
public class PatternParserTest {

    static String OUTPUT_FILE   = "output/PatternParser";
    static String WITNESS_FILE  = "witness/PatternParser";
    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("");

    private static String msgPattern = "%m%n";
    private String mdcMsgPattern1 = "%m : %X%n";
    private String mdcMsgPattern2 = "%m : %X{key1}%n";
    private String mdcMsgPattern3 = "%m : %X{key2}%n";
    private String mdcMsgPattern4 = "%m : %X{key3}%n";
    private String mdcMsgPattern5 = "%m : %X{key1},%X{key2},%X{key3}%n";


    private static final String KEY = "Converter";
    private PatternParser parser;

    @Before
    public void setup() {
        parser = new PatternParser(KEY);
    }

    private void validateConverter(List<PatternConverter> converters, int index, String name) {
        PatternConverter pc = converters.get(index);
        assertEquals("Incorrect converter " + pc.getName() + " at index " + index + " expected " + name,
            pc.getName(), name);
    }

    /**
     * Test the default pattern
     */
    @Test
    public void defaultPattern() {
        List<PatternConverter> converters = parser.parse(msgPattern);
        assertNotNull(converters);
        assertTrue(converters.size() == 2);
        validateConverter(converters, 0, "Message");
        validateConverter(converters, 1, "Line Sep");
    }


}

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

package org.apache.logging.log4j.core.pattern;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the LiteralPatternConverter.
 */
public class LiteralPatternConverterTest {

    @Test
    public void testConvertBackslashes() {
        final String literal = "ABC\\tDEF\\nGHI\\rJKL\\'MNO\\f \\b \\\\DROPPED:\\x";
        final LiteralPatternConverter converter = new LiteralPatternConverter(null, literal, true);
        assertEquals("ABC\tDEF\nGHI\rJKL\'MNO\f \b \\DROPPED:x", converter.getLiteral());
    }

    @Test
    public void testDontConvertBackslashes() {
        final String literal = "ABC\\tDEF\\nGHI\\rJKL\\'MNO\\f \\b \\\\DROPPED:\\x";
        final LiteralPatternConverter converter = new LiteralPatternConverter(null, literal, false);
        assertEquals(literal, converter.getLiteral());
    }

}

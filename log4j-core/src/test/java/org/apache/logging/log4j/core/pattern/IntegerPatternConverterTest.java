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

import java.util.regex.Matcher;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test padding of Integer patterns
 */
public class IntegerPatternConverterTest {

    @Test
    public void testWithSpacePadding() throws Exception {
      final Matcher matcher = IntegerPatternConverter.PATTERN_OPTIONS.matcher("3");
      assertTrue(matcher.matches());
      assertNull(matcher.group("PADDING"));
      assertEquals("3", matcher.group("LENGTH"));
    }

    @Test
    public void testWithZeroPadding() throws Exception {
      final Matcher matcher = IntegerPatternConverter.PATTERN_OPTIONS.matcher("03");
      assertTrue(matcher.matches());
      assertEquals("0", matcher.group("PADDING"));
      assertEquals("3", matcher.group("LENGTH"));
    }

    @Test
    public void testUnmatched() throws Exception {
      final Matcher matcher = IntegerPatternConverter.PATTERN_OPTIONS.matcher("0");
      assertFalse(matcher.matches());
    }
}

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
package org.apache.logging.log4j.taglib;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 */
public class LogTagTest {
    private LogTag tag;

    @Before
    public void setUp() {
        this.tag = new LogTag();
    }

    @Test
    public void testLevel() {
        assertNull("The level should be null (1).", this.tag.getLevel());

        this.tag.setLevel(Level.INFO);
        assertEquals("The level is not correct (1).", Level.INFO, this.tag.getLevel());

        this.tag.init();
        assertNull("The level should be null (2).", this.tag.getLevel());

        this.tag.setLevel("WARN");
        assertEquals("The level is not correct (2).", Level.WARN, this.tag.getLevel());
    }
}

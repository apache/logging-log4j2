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

package org.apache.logging.log4j.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the NanoClockFactory.
 */
public class NanoClockFactoryTest {

    @Test
    public void testDefaultModeIsDummy() {
        assertEquals(NanoClockFactory.Mode.Dummy, NanoClockFactory.getMode());
    }

    @Test
    public void testModeIsMutable() {
        assertEquals(NanoClockFactory.Mode.Dummy, NanoClockFactory.getMode());
        NanoClockFactory.setMode(NanoClockFactory.Mode.System);
        assertEquals(NanoClockFactory.Mode.System, NanoClockFactory.getMode());

        NanoClockFactory.setMode(NanoClockFactory.Mode.Dummy);
        assertEquals(NanoClockFactory.Mode.Dummy, NanoClockFactory.getMode());
    }

    @Test
    public void testModeDeterminesGeneratedClockType() {
        NanoClockFactory.setMode(NanoClockFactory.Mode.Dummy);
        assertEquals(NanoClockFactory.Mode.Dummy, NanoClockFactory.getMode());
        assertTrue("dummy", NanoClockFactory.createNanoClock() instanceof DummyNanoClock);
        
        NanoClockFactory.setMode(NanoClockFactory.Mode.System);
        assertEquals(NanoClockFactory.Mode.System, NanoClockFactory.getMode());
        assertTrue("system", NanoClockFactory.createNanoClock() instanceof SystemNanoClock);
        
        NanoClockFactory.setMode(NanoClockFactory.Mode.Dummy);
        assertEquals(NanoClockFactory.Mode.Dummy, NanoClockFactory.getMode());
        assertTrue("dummy again", NanoClockFactory.createNanoClock() instanceof DummyNanoClock);
        
        NanoClockFactory.setMode(NanoClockFactory.Mode.System);
        assertEquals(NanoClockFactory.Mode.System, NanoClockFactory.getMode());
        assertTrue("system", NanoClockFactory.createNanoClock() instanceof SystemNanoClock);

        NanoClockFactory.setMode(NanoClockFactory.Mode.Dummy);
        assertTrue("dummy again2", NanoClockFactory.createNanoClock() instanceof DummyNanoClock);
    }
}

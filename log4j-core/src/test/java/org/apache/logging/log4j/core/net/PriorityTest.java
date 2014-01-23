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
package org.apache.logging.log4j.core.net;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.junit.Test;

/**
 *
 */
public class PriorityTest {

    @Test
    public void testAuthDebug() {
        final int p = Priority.getPriority(Facility.AUTH, Level.DEBUG);
        assertEquals(39, p);
    }

    @Test
    public void testAuthDiag() {
        final int p = Priority.getPriority(Facility.AUTH, Level.DIAG);
        assertEquals(39, p);
    }

    @Test
    public void testAuthError() {
        final int p = Priority.getPriority(Facility.AUTH, Level.ERROR);
        assertEquals(35, p);
    }

    @Test
    public void testAuthFatal() {
        final int p = Priority.getPriority(Facility.AUTH, Level.FATAL);
        assertEquals(33, p);
    }

    @Test
    public void testAuthInfo() {
        final int p = Priority.getPriority(Facility.AUTH, Level.INFO);
        assertEquals(38, p);
    }

    @Test
    public void testAuthNotice() {
        final int p = Priority.getPriority(Facility.AUTH, Level.NOTICE);
        assertEquals(37, p);
    }

    @Test
    public void testAuthTrace() {
        final int p = Priority.getPriority(Facility.AUTH, Level.TRACE);
        assertEquals(39, p);
    }
    
    @Test
    public void testAuthVerbose() {
        final int p = Priority.getPriority(Facility.AUTH, Level.VERBOSE);
        assertEquals(39, p);
    }

    @Test
    public void testAuthWarn() {
        final int p = Priority.getPriority(Facility.AUTH, Level.WARN);
        assertEquals(36, p);
    }

}

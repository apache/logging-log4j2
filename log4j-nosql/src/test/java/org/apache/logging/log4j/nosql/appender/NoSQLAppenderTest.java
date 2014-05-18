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
package org.apache.logging.log4j.nosql.appender;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class NoSQLAppenderTest {
    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testNoProvider() {
        final NoSQLAppender appender = NoSQLAppender.createAppender("myName01", null, null, null, null);

        assertNull("The appender should be null.", appender);
    }

    @Test
    public void testProvider() {
        @SuppressWarnings("unchecked")
        final NoSQLProvider<?> provider = createStrictMock(NoSQLProvider.class);

        replay(provider);

        final NoSQLAppender appender = NoSQLAppender.createAppender("myName01", null, null, null, provider);

        assertNotNull("The appender should not be null.", appender);
        assertEquals("The toString value is not correct.",
                "myName01{ manager=noSqlManager{ description=myName01, bufferSize=0, provider=" + provider + " } }",
                appender.toString());

        appender.stop();

        verify(provider);
    }

    @Test
    public void testProviderBuffer() {
        @SuppressWarnings("unchecked")
        final NoSQLProvider<?> provider = createStrictMock(NoSQLProvider.class);

        replay(provider);

        final NoSQLAppender appender = NoSQLAppender.createAppender("anotherName02", null, null, "25", provider);

        assertNotNull("The appender should not be null.", appender);
        assertEquals("The toString value is not correct.",
                "anotherName02{ manager=noSqlManager{ description=anotherName02, bufferSize=25, provider=" + provider
                        + " } }", appender.toString());

        appender.stop();

        verify(provider);
    }
}

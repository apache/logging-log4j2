/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.nosql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NoSqlAppenderTest {

    @Mock
    private NoSqlProvider<?> provider;

    @Test
    public void testNoProvider() {
        final NoSqlAppender appender = NoSqlAppender.createAppender("myName01", null, null, null, null);

        assertNull(appender, "The appender should be null.");
    }

    @Test
    public void testProvider() {
        final NoSqlAppender appender = NoSqlAppender.createAppender("myName01", null, null, null, provider);

        assertNotNull(appender, "The appender should not be null.");
        assertEquals(
                "myName01{ manager=noSqlManager{ description=myName01, bufferSize=0, provider=" + provider + " } }",
                appender.toString(),
                "The toString value is not correct.");

        appender.stop();
    }

    @Test
    public void testProviderBuffer() {
        final NoSqlAppender appender = NoSqlAppender.createAppender("anotherName02", null, null, "25", provider);

        assertNotNull(appender, "The appender should not be null.");
        assertEquals(
                "anotherName02{ manager=noSqlManager{ description=anotherName02, bufferSize=25, provider=" + provider
                        + " } }",
                appender.toString(),
                "The toString value is not correct.");

        appender.stop();
    }
}

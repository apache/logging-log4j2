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
package org.apache.logging.log4j.core.appender.nosql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NoSqlAppenderTest {

    @Mock
    private NoSqlProvider<?> provider;

    @Test
    public void testNoProvider() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("myName01").build();

        assertNull(appender, "The appender should be null.");
    }

    @Test
    public void testProvider() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("myName01").setProvider(provider).build();

        assertNotNull(appender, "The appender should not be null.");
        assertEquals(
                "myName01{ manager=noSqlManager{ description=myName01, bufferSize=0, provider=" + provider + " } }",
                appender.toString(), "The toString value is not correct.");

        appender.stop();
    }

    @Test
    public void testProviderBuffer() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("anotherName02").setProvider(provider)
                .setBufferSize(25).build();

        assertNotNull(appender, "The appender should not be null.");
        assertEquals(
                "anotherName02{ manager=noSqlManager{ description=anotherName02, bufferSize=25, provider=" + provider
                        + " } }",
                appender.toString(), "The toString value is not correct.");

        appender.stop();
    }
}

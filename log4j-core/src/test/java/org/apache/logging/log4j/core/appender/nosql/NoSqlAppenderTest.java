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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NoSqlAppenderTest {

    @Mock
    private NoSqlProvider<?> provider;

    @Test
    public void testNoProvider() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("myName01").build();

        assertThat(appender).describedAs("The appender should be null.").isNull();
    }

    @Test
    public void testProvider() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("myName01").setProvider(provider).build();

        assertThat(appender).describedAs("The appender should not be null.").isNotNull();
        assertThat(appender.toString()).describedAs("The toString value is not correct.").isEqualTo("myName01{ manager=noSqlManager{ description=myName01, bufferSize=0, provider=" + provider + " } }");

        appender.stop();
    }

    @Test
    public void testProviderBuffer() {
        final NoSqlAppender appender = NoSqlAppender.newBuilder().setName("anotherName02").setProvider(provider)
                .setBufferSize(25).build();

        assertThat(appender).describedAs("The appender should not be null.").isNotNull();
        assertThat(appender.toString()).describedAs("The toString value is not correct.").isEqualTo("anotherName02{ manager=noSqlManager{ description=anotherName02, bufferSize=25, provider=" + provider
                        + " } }");

        appender.stop();
    }
}

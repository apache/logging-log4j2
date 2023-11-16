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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.jupiter.api.Test;

public class AbstractAppenderBuilderTest {

    @Test
    public void testDefaultLayoutLeak() {
        final int expected = AbstractManager.getManagerCount();
        final Configuration configuration = new DefaultConfiguration();
        final ConsoleAppender appender = ConsoleAppender.newBuilder()
                .setConfiguration(configuration)
                .setName("test")
                .build();
        configuration.addAppender(appender);
        configuration.start();
        configuration.stop();
        assertEquals(expected, AbstractManager.getManagerCount(), "No managers should be left after shutdown.");
    }
}

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
package org.apache.log4j;

import org.apache.log4j.varia.NullAppender;
import org.junit.jupiter.api.Test;

/**
 * Test {@link BasicConfigurator}.
 */
public class BasicConfiguratorTest {

    @Test
    public void testConfigure() {
        // TODO More...
        BasicConfigurator.configure();
    }

    @Test
    public void testResetConfiguration() {
        // TODO More...
        BasicConfigurator.resetConfiguration();
    }

    @Test
    public void testConfigureAppender() {
        BasicConfigurator.configure(null);
        // TODO More...
    }

    @Test
    public void testConfigureConsoleAppender() {
        // TODO What to do? Map to Log4j 2 Appender deeper in the code?
        BasicConfigurator.configure(new ConsoleAppender());
    }

    @Test
    public void testConfigureNullAppender() {
        // The NullAppender name is null and we do not want an NPE when the name is used as a key in a
        // ConcurrentHashMap.
        BasicConfigurator.configure(NullAppender.getNullAppender());
    }
}

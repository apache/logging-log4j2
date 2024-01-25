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
package org.apache.logging.log4j.async.logger;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.LateConfigAbstractTest;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("functional")
@UsingStatusListener
public class AsyncLateConfigTest extends LateConfigAbstractTest {

    @TempLoggingDir
    private static Path loggingPath;

    static Stream<Log4jContextFactory> selectors() {
        final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
        return Stream.<ContextSelector>of(
                        new AsyncLoggerContextSelector(instanceFactory.newChildInstanceFactory()),
                        new BasicAsyncLoggerContextSelector(instanceFactory.newChildInstanceFactory()))
                .map(Log4jContextFactory::new);
    }

    @ParameterizedTest(name = "reconfigure {0}")
    @MethodSource("selectors")
    void reconfiguration(final Log4jContextFactory factory) throws Exception {
        testReconfiguration(factory, loggingPath);
    }
}

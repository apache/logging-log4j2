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
package org.apache.logging.log4j.core.async;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
@UsingStatusListener
@ContextSelectorType(AsyncLoggerContextSelector.class)
public class AsyncLoggerConfigWithAsyncEnabledTest {

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testParametersAreAvailableToLayout(final LoggerContext ctx) throws Exception {
        final File file = loggingPath.resolve("AsyncLoggerConfigTest4.log").toFile();

        final Logger log = ctx.getLogger("com.foo.Bar");
        final String format = "Additive logging: {} for the price of {}!";
        log.info(format, 2, 1);
        ctx.stop(500, TimeUnit.MILLISECONDS);

        final String line1, line2;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
        }
        Files.delete(file.toPath());

        final String expected =
                "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!";
        assertThat(line1, equalTo(expected));
        assertThat(line2, equalTo(expected));
    }
}

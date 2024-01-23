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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
@ContextSelectorType(AsyncLoggerCustomSelectorLocationTest.CustomAsyncContextSelector.class)
public class AsyncLoggerCustomSelectorLocationTest {

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testCustomAsyncSelectorLocation(final LoggerContext ctx) throws Exception {
        final File file =
                loggingPath.resolve("AsyncLoggerCustomSelectorLocationTest.log").toFile();
        final Logger log = ctx.getLogger("com.foo.Bar");
        final Logger logIncludingLocation = ctx.getLogger("com.include.location.Bar");
        final String msg = "Async logger msg with location";
        log.info(msg);
        logIncludingLocation.info(msg);
        ctx.stop(); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String firstLine = reader.readLine();
        final String secondLine = reader.readLine();
        final String thirdLine = reader.readLine();
        reader.close();
        file.delete();
        // By default we expect location to be disabled
        assertThat(firstLine, containsString(msg));
        assertThat(firstLine, not(containsString("testCustomAsyncSelectorLocation")));
        // Configuration allows us to retain location
        assertThat(secondLine, containsString(msg));
        assertThat(secondLine, containsString("testCustomAsyncSelectorLocation"));
        assertThat(thirdLine, nullValue());
    }

    @Singleton
    public static final class CustomAsyncContextSelector implements ContextSelector {
        private static final LoggerContext CONTEXT = new AsyncLoggerContext("AsyncDefault");

        @Override
        public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
            return CONTEXT;
        }

        @Override
        public LoggerContext getContext(
                final String fqcn, final ClassLoader loader, final boolean currentContext, final URI configLocation) {
            return CONTEXT;
        }

        @Override
        public List<LoggerContext> getLoggerContexts() {
            return Collections.singletonList(CONTEXT);
        }

        @Override
        public void removeContext(final LoggerContext context) {
            // does not remove anything
        }

        @Override
        public boolean isClassLoaderDependent() {
            return false;
        }
    }
}

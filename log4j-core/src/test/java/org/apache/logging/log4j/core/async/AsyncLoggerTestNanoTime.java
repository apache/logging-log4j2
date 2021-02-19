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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.time.SystemNanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncLoggerTestNanoTime {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "NanoTimeToFileTest.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Test
    public void testAsyncLogUsesNanoTimeClock() throws Exception {
        final File file = new File("target", "NanoTimeToFileTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final AsyncLogger log = (AsyncLogger) LogManager.getLogger("com.foo.Bar");
        final long before = System.nanoTime();
        log.info("Use actual System.nanoTime()");
        assertThat(log.getNanoClock() instanceof SystemNanoClock).describedAs("using SystemNanoClock").isTrue();

        final long DUMMYNANOTIME = -53;
        log.getContext().getConfiguration().setNanoClock(new DummyNanoClock(DUMMYNANOTIME));
        log.updateConfiguration(log.getContext().getConfiguration());

        // trigger a new nano clock lookup
        log.updateConfiguration(log.getContext().getConfiguration());

        log.info("Use dummy nano clock");
        assertThat(log.getNanoClock() instanceof DummyNanoClock).describedAs("using SystemNanoClock").isTrue();

        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        // System.out.println(line1);
        // System.out.println(line2);
        reader.close();
        file.delete();

        assertThat(line1).describedAs("line1").isNotNull();
        assertThat(line2).describedAs("line2").isNotNull();
        final String[] line1Parts = line1.split(" AND ");
        assertThat(line1Parts[2]).isEqualTo("Use actual System.nanoTime()");
        assertThat(line1Parts[1]).isEqualTo(line1Parts[0]);
        final long loggedNanoTime = Long.parseLong(line1Parts[0]);
        assertThat(loggedNanoTime - before).describedAs("used system nano time").isLessThan(TimeUnit.SECONDS.toNanos(1));

        final String[] line2Parts = line2.split(" AND ");
        assertThat(line2Parts[2]).isEqualTo("Use dummy nano clock");
        assertThat(line2Parts[0]).isEqualTo(String.valueOf(DUMMYNANOTIME));
        assertThat(line2Parts[1]).isEqualTo(String.valueOf(DUMMYNANOTIME));
    }

}

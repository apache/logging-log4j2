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
package org.apache.logging.log4j.core.appender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class RollingRandomAccessFileAppenderRolloverTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "RollingRandomAccessFileAppenderTest.xml");
    }

    @Test
    @Ignore
    public void testRollover() throws Exception {
        final File file = new File("target", "RollingRandomAccessFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        final File after1 = new File("target", "afterRollover-1.log");
        file.delete();
        after1.delete();

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "First a short message that does not trigger rollover";
        log.info(msg);
        Thread.sleep(50);

        BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        assertThat(line1.contains(msg)).isTrue();
        reader.close();

        assertThat(after1.exists()).describedAs("afterRollover-1.log not created yet").isFalse();

        String exceed = "Long message that exceeds rollover size... ";
        final char[] padding = new char[250];
        Arrays.fill(padding, 'X');
        exceed += new String(padding);
        log.warn(exceed);
        assertThat(after1.exists()).describedAs("exceeded size but afterRollover-1.log not created yet").isFalse();

        final String trigger = "This message triggers rollover.";
        log.warn(trigger);
        Thread.sleep(100);
        log.warn(trigger);

        CoreLoggerContexts.stopLoggerContext(); // stop async thread
        CoreLoggerContexts.stopLoggerContext(false); // stop async thread

        final int MAX_ATTEMPTS = 50;
        int count = 0;
        while (!after1.exists() && count++ < MAX_ATTEMPTS) {
            Thread.sleep(50);
        }

        assertThat(after1.exists()).describedAs("afterRollover-1.log created").isTrue();

        reader = new BufferedReader(new FileReader(file));
        final String new1 = reader.readLine();
        assertThat(new1.contains(trigger)).describedAs("after rollover only new msg").isTrue();
        assertThat(reader.readLine()).describedAs("No more lines").isNull();
        reader.close();
        file.delete();

        reader = new BufferedReader(new FileReader(after1));
        final String old1 = reader.readLine();
        assertThat(old1.contains(msg)).describedAs("renamed file line 1").isTrue();
        final String old2 = reader.readLine();
        assertThat(old2.contains(exceed)).describedAs("renamed file line 2").isTrue();
        String line = reader.readLine();
        if (line != null) {
            assertThat(line.contains("This message triggers rollover.")).describedAs("strange...").isTrue();
            line = reader.readLine();
        }
        assertThat(line).describedAs("No more lines").isNull();
        reader.close();
        after1.delete();
    }
}

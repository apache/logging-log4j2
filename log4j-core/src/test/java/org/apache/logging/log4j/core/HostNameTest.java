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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-test2.xml")
public class HostNameTest {

    private final ListAppender host;
    private final RollingFileAppender hostFile;

    public HostNameTest(@Named("HostTest") final ListAppender list, @Named("HostFile") final RollingFileAppender rolling) {
        host = list.clear();
        hostFile = rolling;
    }

    @Test
    public void testHostname(final LoggerContext context) {
        final org.apache.logging.log4j.Logger testLogger = context.getLogger("org.apache.logging.log4j.hosttest");
        testLogger.debug("Hello, {}", "World");
        final List<String> msgs = host.getMessages();
        assertThat(msgs).hasSize(1);
        String expected = NetUtils.getLocalHostname() + Strings.LINE_SEPARATOR;
        assertThat(msgs.get(0)).endsWith(expected);
        assertThat(hostFile.getFileName()).describedAs("No Host FileAppender file name").isNotNull();
        expected = "target/" + NetUtils.getLocalHostname() + ".log";
        String name = hostFile.getFileName();
        assertThat(expected).describedAs("Incorrect HostFile FileAppender file name - expected " + expected + " actual - " + name).isEqualTo(name);
        name = hostFile.getFilePattern();
        assertThat(name).describedAs("No file pattern").isNotNull();
        expected = "target/" + NetUtils.getLocalHostname() + "-%d{MM-dd-yyyy}-%i.log";
        assertThat(expected).describedAs("Incorrect HostFile FileAppender file pattern - expected " + expected + " actual - " + name).isEqualTo(name);

    }
}

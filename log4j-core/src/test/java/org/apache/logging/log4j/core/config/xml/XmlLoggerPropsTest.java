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
package org.apache.logging.log4j.core.config.xml;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class XmlLoggerPropsTest {

    @BeforeAll
    static void setupClass() {
        System.setProperty("test", "test");
    }

    @AfterAll
    static void tearDownClass() {
        System.clearProperty("test");
    }

    @Test
    @LoggerContextSource("log4j-loggerprops.xml")
    public void testWithProps(final LoggerContext context, @Named("List") final ListAppender listAppender) {
        assertThat(context.getConfiguration(), is(instanceOf(XmlConfiguration.class)));
        context.getLogger(getClass()).debug("Test with props");
        context.getLogger("tiny.bubbles").debug("Test on root");
        final List<String> events = listAppender.getMessages();
        listAppender.clear();
        assertThat(events, hasSize(2));
        assertThat(events.get(0), allOf(
                containsString("user="),
                containsString("phrasex=****"),
                containsString("test=test"),
                containsString("test2=test2default"),
                containsString("test3=Unknown"),
                containsString("test4=test"),
                containsString("test5=test"),
                containsString("attribKey=attribValue"),
                containsString("duplicateKey=nodeValue")
        ));
        assertThat(events.get(1), allOf(
                containsString("user="),
                containsString("phrasex=****"),
                containsString("test=test"),
                containsString("test2=test2default"),
                containsString("test3=Unknown"),
                containsString("test4=test"),
                containsString("test5=test"),
                containsString("attribKey=attribValue"),
                containsString("duplicateKey=nodeValue")
        ));
    }
}

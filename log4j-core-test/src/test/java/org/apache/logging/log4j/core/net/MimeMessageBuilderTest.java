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
package org.apache.logging.log4j.core.net;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link MimeMessageBuilder} header sanitization.
 */
class MimeMessageBuilderTest {

    @Test
    void testSubjectWithoutLineBreaksIsUnchanged() throws Exception {
        MimeMessageBuilder builder = new MimeMessageBuilder(Session.getInstance(new Properties()));
        MimeMessage message = builder.setSubject("Error in app").build();
        assertThat(message.getSubject()).isEqualTo("Error in app");
    }

    @Test
    void testSubjectLineBreaksAreStripped() throws Exception {
        MimeMessageBuilder builder = new MimeMessageBuilder(Session.getInstance(new Properties()));
        MimeMessage message = builder.setSubject("Error\r\nBcc: victim@example.com").build();
        // CR and LF are stripped, so the remainder is inert text within the single
        // subject value and cannot start a new header.
        assertThat(message.getSubject())
                .doesNotContain("\r")
                .doesNotContain("\n");
    }
}

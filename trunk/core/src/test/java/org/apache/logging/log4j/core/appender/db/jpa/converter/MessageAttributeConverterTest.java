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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageAttributeConverterTest {
    private static final StatusLogger log = StatusLogger.getLogger();

    private MessageAttributeConverter converter;

    @Before
    public void setUp() {
        this.converter = new MessageAttributeConverter();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testConvert01() {
        Message message = log.getMessageFactory().newMessage("Message #{} said [{}].", 3, "Hello");

        String converted = this.converter.convertToDatabaseColumn(message);

        assertNotNull("The converted value should not be null.", converted);
        assertEquals("The converted value is not correct.", "Message #3 said [Hello].", converted);

        Message reversed = this.converter.convertToEntityAttribute(converted);

        assertNotNull("The reversed value should not be null.", reversed);
        assertEquals("The reversed value is not correct.", "Message #3 said [Hello].", reversed.getFormattedMessage());
    }
}

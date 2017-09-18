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

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * <b>Consider this class private.</b>
 * <p>
 * Transforms the specified user message to append an internal Log4j2 message explaining why this message appears out
 * of order in the appender.
 * </p>
 */
public class AsyncQueueFullMessageUtil {
    /**
     * Returns a new {@code Message} based on the original message that appends an internal Log4j2 message
     * explaining why this message appears out of order in the appender.
     * <p>
     * Any parameter objects present in the original message are not included in the returned message.
     * </p>
     * @param message the message to replace
     * @return a new {@code Message} object
     */
    public static Message transform(Message message) {
        SimpleMessage result = new SimpleMessage(message.getFormattedMessage() +
                " (Log4j2 logged this message out of order to prevent deadlock caused by domain " +
                "objects logging from their toString method when the async queue is full - LOG4J2-2031)");
        return result;
    }
}

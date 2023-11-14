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

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;

/**
 * Helper class providing some async logging-related functionality.
 * <p>
 *     Consider this class private.
 * </p>
 */
public class InternalAsyncUtil {
    /**
     * Returns the specified message, with its content frozen unless system property
     * {@code log4j.format.msg.async} is true or the message class is annotated with
     * {@link AsynchronouslyFormattable}.
     *
     * @param msg the message object to inspect, modify and return
     * @return Returns the specified message, with its content frozen
     */
    public static Message makeMessageImmutable(final Message msg) {
        // if the Message instance is reused, there is no point in freezing its message here
        if (msg != null && !canFormatMessageInBackground(msg)) {
            msg.getFormattedMessage(); // LOG4J2-763: ask message to makeMessageImmutable parameters
        }
        return msg;
    }

    private static boolean canFormatMessageInBackground(final Message message) {
        return Constants.FORMAT_MESSAGES_IN_BACKGROUND // LOG4J2-898: user wants to format all msgs in background
                || message.getClass().isAnnotationPresent(AsynchronouslyFormattable.class); // LOG4J2-1718
    }
}

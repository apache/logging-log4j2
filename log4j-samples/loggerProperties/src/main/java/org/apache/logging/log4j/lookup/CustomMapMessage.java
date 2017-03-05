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
package org.apache.logging.log4j.lookup;

import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.MapMessage;

import java.util.Map;

/**
 *
 */
@AsynchronouslyFormattable
public class CustomMapMessage extends MapMessage {

    private static final long serialVersionUID = 1L;
    private final String message;

    public CustomMapMessage(final String msg, final Map<String, String> map) {
        super(map);
        this.message = msg;
    }

    @Override
    public String getFormattedMessage(final String[] formats) {
        if (message != null) {
            return message;
        }
        return super.getFormattedMessage(formats);
    }

    @Override
    public String getFormattedMessage() {
        if (message != null) {
            return message;
        }
        return super.getFormattedMessage();
    }

}

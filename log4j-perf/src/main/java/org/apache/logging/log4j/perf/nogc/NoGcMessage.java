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
package org.apache.logging.log4j.perf.nogc;

import org.apache.logging.log4j.message.Message;

import java.util.Arrays;

/**
 * Reusable Message..
 */
public class NoGcMessage implements Message {
    private Object[] params = new Object[10];
    private int paramCount;
    private StringBuilder buffer = new StringBuilder(2048);

    public NoGcMessage() {
    }

    public StringBuilder get() {
        return buffer;
    }

    public StringBuilder set(String message, Object p1, Object p2, Object p3, Object p4) {
        params[0] = p1;
        params[1] = p2;
        params[2] = p3;
        params[3] = p4;
        paramCount = 4;
        int current = 0;
        buffer.setLength(0);
        for (int i = 0; i < message.length() - 1; i++) {
            char c = message.charAt(i);
            if (c == '{' && message.charAt(i + 1) == '}') {
                buffer.append(params[current++]);
                i++;
            } else {
                buffer.append(c);
            }
        }
        char c = message.charAt(message.length() - 1);
        if (c != '}') {
            buffer.append(c);
        }
        return buffer;
    }

    @Override
    public String getFormattedMessage() {
        return buffer.toString(); // not called by NoGcLayout
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return Arrays.copyOf(params, paramCount);
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}

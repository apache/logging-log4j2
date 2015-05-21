/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.dumbster.smtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class SmtpMessage {
    /**
     * Headers: Map of List of String hashed on header name.
     */
    private final Map<String, List<String>> headers;
    /**
     * Message body.
     */
    private final StringBuffer body;

    /**
     * Constructor. Initializes headers Map and body buffer.
     */
    public SmtpMessage() {
        headers = new HashMap<>(10);
        body = new StringBuffer();
    }

    /**
     * Update the headers or body depending on the SmtpResponse object and line of input.
     *
     * @param response SmtpResponse object
     * @param params   remainder of input line after SMTP command has been removed
     */
    public void store(final SmtpResponse response, final String params) {
        if (params != null) {
            if (SmtpState.DATA_HDR.equals(response.getNextState())) {
                final int headerNameEnd = params.indexOf(':');
                if (headerNameEnd >= 0) {
                    final String name = params.substring(0, headerNameEnd).trim();
                    final String value = params.substring(headerNameEnd + 1).trim();
                    addHeader(name, value);
                }
            } else if (SmtpState.DATA_BODY == response.getNextState()) {
                body.append(params);
            }
        }
    }

    /**
     * Get an Iterator over the header names.
     *
     * @return an Iterator over the set of header names (String)
     */
    public Iterator<String> getHeaderNames() {
        final Set<String> nameSet = headers.keySet();
        return nameSet.iterator();
    }

    /**
     * Get the value(s) associated with the given header name.
     *
     * @param name header name
     * @return value(s) associated with the header name
     */
    public String[] getHeaderValues(final String name) {
        final List<String> values = headers.get(name);
        if (values == null) {
            return new String[0];
        }
        return values.toArray(new String[values.size()]);
    }

    /**
     * Get the first values associated with a given header name.
     *
     * @param name header name
     * @return first value associated with the header name
     */
    public String getHeaderValue(final String name) {
        final List<String> values = headers.get(name);
        if (values == null) {
            return null;
        }
        final Iterator<String> iterator = values.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Get the message body.
     *
     * @return message body
     */
    public String getBody() {
        return body.toString();
    }

    /**
     * Adds a header to the Map.
     *
     * @param name  header name
     * @param value header value
     */
    private void addHeader(final String name, final String value) {
        List<String> valueList = headers.get(name);
        if (valueList == null) {
            valueList = new ArrayList<>(1);
            headers.put(name, valueList);
        }
        valueList.add(value);
    }

    /**
     * String representation of the SmtpMessage.
     *
     * @return a String
     */
    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            final String name = entry.getKey();
            final List<String> values = entry.getValue();
            for (final String value : values) {
                msg.append(name);
                msg.append(": ");
                msg.append(value);
                msg.append('\n');
            }
        }
        msg.append('\n');
        msg.append(body);
        msg.append('\n');
        return msg.toString();
    }
}

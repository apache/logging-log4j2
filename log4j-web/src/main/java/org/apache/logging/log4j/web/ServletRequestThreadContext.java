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
package org.apache.logging.log4j.web;

import java.util.Objects;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.ThreadContext;

public class ServletRequestThreadContext {

    public static void put(final String key, final ServletRequest servletRequest) {
        put(key, "RemoteAddr", servletRequest.getRemoteAddr());
        put(key, "RemoteHost", servletRequest.getRemoteHost());
        put(key, "RemotePort", servletRequest.getRemotePort());
    }

    public static void put(final String key, final String field, final Object value) {
        put(key + "." + field, Objects.toString(value));
    }

    public static void put(final String key, final String value) {
        ThreadContext.put(key, value);
    }

    public static void put(final String key, final HttpServletRequest servletRequest) {
        put(key, (ServletRequest) servletRequest);
    }
}

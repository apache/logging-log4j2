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
package org.apache.logging.log4j.util;

import java.nio.charset.Charset;
import java.util.Base64;

/**
 * Base64 encodes Strings. This utility is only necessary because the mechanism to do this changed in Java 8 and
 * the original method for Base64 encoding was removed in Java 9.
 */
public final class Base64Util {

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private Base64Util() {}

    public static String encode(final String str) {
        return str != null ? encoder.encodeToString(str.getBytes(Charset.defaultCharset())) : null;
    }
}

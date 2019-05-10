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
package org.apache.logging.log4j.util;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Base64 encodes Strings. This utility is only necessary because the mechanism to do this changed in Java 8 and
 * the original method was removed in Java 9.
 */
public final class Base64Util {

    private static Method encodeMethod = null;
    private static Object encoder = null;

    static {
        try {
            Class<?> clazz = LoaderUtil.loadClass("java.util.Base64");
            Class<?> encoderClazz = LoaderUtil.loadClass("java.util.Base64$Encoder");
            Method method = clazz.getMethod("getEncoder");
            encoder = method.invoke(null);
            encodeMethod = encoderClazz.getMethod("encodeToString", byte[].class);
        } catch (Exception ex) {
            try {
                Class<?> clazz = LoaderUtil.loadClass("javax.xml.bind.DataTypeConverter");
                encodeMethod = clazz.getMethod("printBase64Binary");
            } catch (Exception ex2) {
                LowLevelLogUtil.logException("Unable to create a Base64 Encoder", ex2);
            }
        }
    }

    private Base64Util() {
    }

    public static String encode(String str) {
        if (str == null) {
            return null;
        }
        byte [] data = str.getBytes();
        if (encodeMethod != null) {
            try {
                return (String) encodeMethod.invoke(encoder, data);
            } catch (Exception ex) {
                throw new LoggingException("Unable to encode String", ex);
            }
        }
        throw new LoggingException("No Encoder, unable to encode string");
    }
}

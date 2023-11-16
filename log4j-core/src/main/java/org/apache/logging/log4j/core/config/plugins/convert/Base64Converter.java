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
package org.apache.logging.log4j.core.config.plugins.convert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * @since 2.9
 */
public class Base64Converter {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static Method method = null;
    private static Object decoder = null;

    static {
        try {
            // Base64 is available in Java 8 and up.
            Class<?> clazz = LoaderUtil.loadClass("java.util.Base64");
            final Method getDecoder = clazz.getMethod("getDecoder", (Class[]) null);
            decoder = getDecoder.invoke(null, (Object[]) null);
            clazz = decoder.getClass();
            method = clazz.getMethod("decode", String.class);
        } catch (final ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException ex) {
            // ignore
        }
        if (method == null) {
            try {
                // DatatypeConverter is not in the default module in Java 9.
                final Class<?> clazz = LoaderUtil.loadClass("javax.xml.bind.DatatypeConverter");
                method = clazz.getMethod("parseBase64Binary", String.class);
            } catch (final ClassNotFoundException ex) {
                LOGGER.error("No Base64 Converter is available");
            } catch (final NoSuchMethodException ex) {

            }
        }
    }

    public static byte[] parseBase64Binary(final String encoded) {
        if (method == null) {
            LOGGER.error("No base64 converter");
        } else {
            try {
                return (byte[]) method.invoke(decoder, encoded);
            } catch (final IllegalAccessException | InvocationTargetException ex) {
                LOGGER.error("Error decoding string - " + ex.getMessage());
            }
        }
        return Constants.EMPTY_BYTE_ARRAY;
    }
}

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
package org.apache.logging.log4j.core.net;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.STREAM_VERSION;
import static java.io.ObjectStreamConstants.TC_ARRAY;
import static java.io.ObjectStreamConstants.TC_CLASS;
import static java.io.ObjectStreamConstants.TC_CLASSDESC;
import static java.io.ObjectStreamConstants.TC_ENUM;
import static java.io.ObjectStreamConstants.TC_LONGSTRING;
import static java.io.ObjectStreamConstants.TC_OBJECT;
import static java.io.ObjectStreamConstants.TC_PROXYCLASSDESC;
import static java.io.ObjectStreamConstants.TC_REFERENCE;
import static java.io.ObjectStreamConstants.TC_STRING;

// See https://docs.oracle.com/javase/7/docs/platform/serialization/spec/protocol.html for
// specification of the Object Serialization Stream Protocol
final class SerializationHelper {
    // Return the class name of serializedObject, or null if the class name can not be extracted.
    static String extractClassName(final byte[] serializedObject) {
        ByteBuffer buffer = ByteBuffer.wrap(serializedObject);
        try {
            if (buffer.getShort() != STREAM_MAGIC) {
                return null;
            }
            if (buffer.getShort() != STREAM_VERSION) {
                return null;
            }
            byte tag = buffer.get();
            switch (tag) {
                case TC_STRING:
                case TC_LONGSTRING:
                    return String.class.getName();
                case TC_ENUM:
                case TC_OBJECT:
                    byte classDescType = buffer.get();
                    if (classDescType == TC_CLASSDESC) {
                        short stringLength = buffer.getShort();
                        return new String(serializedObject, buffer.position(), stringLength, StandardCharsets.UTF_8);
                    } else if (classDescType == TC_PROXYCLASSDESC) {
                        // Don't support proxies, at least for now
                        return null;
                    }
                case TC_REFERENCE:
                case TC_CLASSDESC:
                case TC_CLASS:
                case TC_ARRAY:
                default:
                    return null;
            }
        } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private SerializationHelper() {
    }
}

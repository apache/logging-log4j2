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

package org.apache.logging.log4j.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility class to facilitate serializing and deserializing objects.
 */
public class SerialUtil {
    private SerialUtil() {
    }

    /**
     * Serializes the specified object and returns the result as a byte array.
     * @param obj the object to serialize
     * @return the serialized object
     */
    public static byte[] serialize(final Serializable obj) {
        try {
            final ByteArrayOutputStream bas = new ByteArrayOutputStream(8192);
            final ObjectOutputStream oos = new ObjectOutputStream(bas);
            oos.writeObject(obj);
            oos.flush();
            return bas.toByteArray();
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not serialize", ex);
        }
    }
    
    /**
     * Deserialize an object from the specified byte array and returns the result.
     * @param data byte array representing the serialized object
     * @return the deserialized object
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final byte[] data) {
        try {
            final ByteArrayInputStream bas = new ByteArrayInputStream(data);
            final ObjectInputStream ois = new ObjectInputStream(bas);
            return (T) ois.readObject();
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not deserialize", ex);
        }
    }
}

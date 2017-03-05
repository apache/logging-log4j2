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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * Deserializes a specified file.
 *
 * @see SortedArrayStringMapTest#testDeserializationOfUnknownClass()
 */
public class DeserializerHelper {
    public static void main(final String... args) throws Exception {
        final File file = new File(args[0]);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            final Object result = in.readObject();
            System.out.println(result);
        } catch (final Throwable t) {
            System.err.println("Could not deserialize.");
            throw t; // cause non-zero exit code
        } finally {
            try {
                in.close();
            } catch (final Throwable t) {
                System.err.println("Error while closing: " + t);
            }
        }
    }
}

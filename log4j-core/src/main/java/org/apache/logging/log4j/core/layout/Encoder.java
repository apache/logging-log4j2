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
package org.apache.logging.log4j.core.layout;

/**
 * Objects implementing the {@code Encoder} interface know how to convert an object to some binary representation and
 * write the result to a {@code ByteBuffer}, ideally without creating temporary objects.
 *
 * @param <T> the type of objects that the Encoder can encode
 * @since 2.6
 */
public interface Encoder<T> {

    /**
     * Encodes the specified source object to some binary representation and writes the result to the specified
     * destination.
     *
     * @param source the object to encode.
     * @param destination holds the ByteBuffer to write into.
     */
    void encode(T source, ByteBufferDestination destination);
}

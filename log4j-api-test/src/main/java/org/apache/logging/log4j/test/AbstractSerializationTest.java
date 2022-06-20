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
package org.apache.logging.log4j.test;

import static org.apache.logging.log4j.test.SerializableMatchers.serializesRoundTrip;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Subclasses tests {@link Serializable} objects.
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractSerializationTest {

    protected abstract Stream<Object> data();

    @ParameterizedTest
    @MethodSource("data")
    public void testSerializationRoundtripEquals(Serializable serializable) {
        assertThat(serializable, serializesRoundTrip(serializable));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testSerializationRoundtripNoException(Serializable serializable) {
        assertThat(serializable, serializesRoundTrip());
    }
}

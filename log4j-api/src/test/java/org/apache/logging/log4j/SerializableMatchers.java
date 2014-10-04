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
package org.apache.logging.log4j;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.any;

/**
 * Hamcrest Matchers for Serializable classes.
 *
 * @since 2.1
 */
public final class SerializableMatchers {

    public static <T extends Serializable> Matcher<T> serializesRoundTrip(final Matcher<T> matcher) {
        return new FeatureMatcher<T, T>(matcher, "serializes round trip", "serializes round trip") {
            @Override
            protected T featureValueOf(final T actual) {
                return SerializationUtils.roundtrip(actual);
            }
        };
    }

    public static <T extends Serializable> Matcher<T> serializesRoundTrip(final T expected) {
        return serializesRoundTrip(equalTo(expected));
    }

    public static <T extends Serializable> Matcher<T> serializesRoundTrip(final Class<T> clazz) {
        return serializesRoundTrip(any(clazz));
    }

    public static Matcher<? super Serializable> serializesRoundTrip() {
        return serializesRoundTrip(any(Serializable.class));
    }

    private SerializableMatchers() {
    }
}

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
package org.apache.logging.log4j.hamcrest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

/**
 * Grammatical descriptor decorators for Matchers.
 *
 * @since 2.1
 */
public class Descriptors {
    /**
     * Decorating Matcher similar to {@code is()}, but for better grammar.
     *
     * @param matcher the Matcher to decorate.
     * @param <T> the type expected by the Matcher.
     * @return the decorated Matcher.
     */
    public static <T> Matcher<T> that(final Matcher<T> matcher) {
        return new Is<T>(matcher) {
            @Override
            public void describeTo(final Description description) {
                description.appendText("that ").appendDescriptionOf(matcher);
            }
        };
    }
}

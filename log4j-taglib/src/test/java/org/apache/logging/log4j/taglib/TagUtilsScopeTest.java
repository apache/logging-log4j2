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
package org.apache.logging.log4j.taglib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import javax.servlet.jsp.PageContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TagUtilsScopeTest {

    public static Stream<Arguments> testGetScope() {
        return Stream.of(
                Arguments.of(PageContext.APPLICATION_SCOPE, "application"),
                Arguments.of(PageContext.PAGE_SCOPE, "page"),
                Arguments.of(PageContext.REQUEST_SCOPE, "request"),
                Arguments.of(PageContext.SESSION_SCOPE, "session"),
                Arguments.of(PageContext.PAGE_SCOPE, "insert random garbage here"));
    }

    @ParameterizedTest
    @MethodSource()
    public void testGetScope(final int scope, final String scopeName) throws Exception {
        assertEquals(scope, TagUtils.getScope(scopeName), "The scope is not correct.");
    }
}

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

import java.util.Arrays;
import java.util.Collection;
import javax.servlet.jsp.PageContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TagUtilsScopeTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {PageContext.APPLICATION_SCOPE, "application"},
            {PageContext.PAGE_SCOPE, "page"},
            {PageContext.REQUEST_SCOPE, "request"},
            {PageContext.SESSION_SCOPE, "session"},
            {PageContext.PAGE_SCOPE, "insert random garbage here"}
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    void testGetScope(final int scope, final String scopeName) {
        assertEquals(scope, TagUtils.getScope(scopeName), "The scope is not correct.");
    }
}

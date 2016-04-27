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
package org.apache.logging.log4j.taglib;

import java.util.Arrays;
import java.util.Collection;
import javax.servlet.jsp.PageContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TagUtilsScopeTest {

    private final int scope;
    private final String scopeName;

    public TagUtilsScopeTest(final int scope, final String scopeName) {
        this.scope = scope;
        this.scopeName = scopeName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {PageContext.APPLICATION_SCOPE, "application"},
            {PageContext.PAGE_SCOPE, "page"},
            {PageContext.REQUEST_SCOPE, "request"},
            {PageContext.SESSION_SCOPE, "session"},
            {PageContext.PAGE_SCOPE, "insert random garbage here"}
        });
    }

    @Test
    public void testGetScope() throws Exception {
        assertEquals("The scope is not correct.", scope, TagUtils.getScope(scopeName));
    }

}

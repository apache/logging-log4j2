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
package org.apache.log4j;

import java.util.Stack;
import org.apache.logging.log4j.util.Strings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class NDCTest {

    @Test
    public void testPopEmpty() {
        NDC.clear();
        assertEquals(Strings.EMPTY, NDC.pop());
    }

    @Test
    public void testPeekEmpty() {
        NDC.clear();
        assertEquals(Strings.EMPTY, NDC.peek());
    }

    @SuppressWarnings({"rawtypes"})
    @Test
    public void testCompileCloneToInherit() {
        NDC.inherit(NDC.cloneStack());
        final Stack stackRaw = NDC.cloneStack();
        NDC.inherit(stackRaw);
        final Stack<?> stackAny = NDC.cloneStack();
        NDC.inherit(stackAny);
    }
}

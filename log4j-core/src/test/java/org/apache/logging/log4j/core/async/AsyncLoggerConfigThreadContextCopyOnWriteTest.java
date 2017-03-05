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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.categories.AsyncLoggers;
import org.junit.experimental.categories.Category;

// Note: the different ThreadContextMap implementations cannot be parameterized:
// ThreadContext initialization will result in static final fields being set in various components.
// To use a different ThreadContextMap, the test needs to be run in a new JVM.
@Category(AsyncLoggers.class)
public class AsyncLoggerConfigThreadContextCopyOnWriteTest extends AbstractAsyncThreadContextTestBase {
    public AsyncLoggerConfigThreadContextCopyOnWriteTest() {
        super(ContextImpl.COPY_ON_WRITE, Mode.MIXED);
    }
}

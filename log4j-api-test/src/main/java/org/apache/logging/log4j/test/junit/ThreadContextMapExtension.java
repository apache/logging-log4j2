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
package org.apache.logging.log4j.test.junit;

import java.util.Map;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class ThreadContextMapExtension implements BeforeEachCallback {
    private static final class ThreadContextMapStore implements ExtensionContext.Store.CloseableResource {
        private final Map<String, String> previousMap = ThreadContext.getImmutableContext();

        private ThreadContextMapStore() {
            ThreadContext.clearMap();
        }

        @Override
        public void close() throws Throwable {
            // TODO LOG4J2-1517 Add ThreadContext.setContext(Map<String, String>)
            ThreadContext.clearMap();
            ThreadContext.putAll(previousMap);
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        context.getStore(ExtensionContextAnchor.LOG4J2_NAMESPACE).getOrComputeIfAbsent(ThreadContextMapStore.class);
    }
}

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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.junit.jupiter.api.Test;

public class ReusableParameterizedMessageMemoryLeakTest {

    @Test
    public void testParametersAreNotLeaked() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ReusableMessage message = (ReusableMessage)
                ReusableMessageFactory.INSTANCE.newMessage("foo {}", new ParameterObject("paramValue", latch));
        // Large enough for the parameters, but smaller than the default reusable array size.
        message.swapParameters(new Object[5]);
        final GarbageCollectionHelper gcHelper = new GarbageCollectionHelper();
        gcHelper.run();
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Parameter should have been garbage collected");
        } finally {
            gcHelper.close();
        }
    }

    private static final class ParameterObject {
        private final String value;
        private final CountDownLatch latch;

        ParameterObject(final String value, final CountDownLatch latch) {
            this.value = value;
            this.latch = latch;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        protected void finalize() throws Throwable {
            latch.countDown();
            super.finalize();
        }
    }
}

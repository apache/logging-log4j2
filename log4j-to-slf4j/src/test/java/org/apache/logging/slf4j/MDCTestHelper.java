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
package org.apache.logging.slf4j;

import java.lang.reflect.Field;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

class MDCTestHelper {

    static MDCAdapter replaceMDCAdapter(final MDCAdapter adapter) throws Exception {
        Field mdcAdapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
        mdcAdapterField.setAccessible(true);
        final MDCAdapter old = (MDCAdapter) mdcAdapterField.get(null);
        mdcAdapterField.set(null, adapter);
        return old;
    }
}

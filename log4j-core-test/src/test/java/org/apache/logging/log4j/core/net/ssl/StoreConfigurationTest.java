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
package org.apache.logging.log4j.core.net.ssl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class StoreConfigurationTest<T extends StoreConfiguration<?>> {

    @Test
    void equalsWithNotNullValues() {
        final String location = "/to/the/file.jks";
        final PasswordProvider password = new MemoryPasswordProvider("changeit".toCharArray());
        final StoreConfiguration<Object> a = new StoreConfiguration<>(location, password);
        final StoreConfiguration<Object> b = new StoreConfiguration<>(location, password);

        assertEquals(b, a);
        assertEquals(a, b);
    }

    @Test
    void notEqualsWithNullAndNotNullValues() {
        final String location = "/to/the/file.jks";
        final PasswordProvider password = new MemoryPasswordProvider("changeit".toCharArray());
        final StoreConfiguration<Object> a = new StoreConfiguration<>(location, password);
        final StoreConfiguration<Object> b = new StoreConfiguration<>(null, new MemoryPasswordProvider(null));

        assertNotEquals(a, b);
        assertNotEquals(b, a);
    }

    @Test
    void equalsWithNullValues() {
        final StoreConfiguration<Object> a = new StoreConfiguration<>(null, new MemoryPasswordProvider(null));
        final StoreConfiguration<Object> b = new StoreConfiguration<>(null, new MemoryPasswordProvider(null));

        assertEquals(b, a);
        assertEquals(a, b);
    }
}

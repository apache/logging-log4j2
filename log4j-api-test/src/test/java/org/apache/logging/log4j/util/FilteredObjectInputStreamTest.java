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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.Test;

class FilteredObjectInputStreamTest {

    private static FilteredObjectInputStream filteredStream(final byte[] data) throws Exception {
        return new FilteredObjectInputStream(new ByteArrayInputStream(data));
    }

    @Test
    void allow_listed_object_graph_round_trips() throws Exception {
        final Map<String, Object> original = new HashMap<>();
        original.put("string", "value");
        original.put("integer", 17);
        original.put("array", new int[] {1, 2, 3});
        final Object restored =
                filteredStream(SerialUtil.serialize((Serializable) original)).readObject();
        assertEquals(original.keySet(), ((Map<?, ?>) restored).keySet());
    }

    @Test
    void class_outside_allow_list_is_rejected() throws Exception {
        final byte[] data = SerialUtil.serialize(new File("rejected"));
        assertThrows(InvalidObjectException.class, () -> filteredStream(data).readObject());
    }

    @Test
    void dynamic_proxy_is_rejected() throws Exception {
        final byte[] data = SerialUtil.serialize((Serializable) serializableProxy());
        assertThrows(InvalidObjectException.class, () -> filteredStream(data).readObject());
    }

    @Test
    void allowed_extra_classes_do_not_re_enable_proxies() throws Exception {
        final Object proxy = serializableProxy();
        final byte[] data = SerialUtil.serialize((Serializable) proxy);
        final FilteredObjectInputStream stream = new FilteredObjectInputStream(
                new ByteArrayInputStream(data),
                Collections.singleton(proxy.getClass().getName()));
        assertThrows(InvalidObjectException.class, stream::readObject);
    }

    private static Object serializableProxy() {
        return Proxy.newProxyInstance(
                FilteredObjectInputStreamTest.class.getClassLoader(),
                new Class<?>[] {Comparator.class},
                new SerializableInvocationHandler());
    }

    private static class SerializableInvocationHandler implements InvocationHandler, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            return null;
        }
    }
}

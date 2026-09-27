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
package org.apache.logging.log4j.core.test.junit;

import java.util.Collections;
import java.util.Map;
import javax.naming.Context;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * JUnit Extension to create a mock {@link Context} and bind an object to a name.
 *
 */
public class JndiExtension implements BeforeEachCallback {

    private final Map<String, Object> initialBindings;

    public JndiExtension(final String name, final Object value) {
        this.initialBindings = Collections.singletonMap(name, value);
    }

    public JndiExtension(final Map<String, Object> initialBindings) {
        this.initialBindings = initialBindings;
    }

    public void beforeEach(ExtensionContext ctx) throws Exception {
        final SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        for (final Map.Entry<String, Object> entry : initialBindings.entrySet()) {
            builder.bind(entry.getKey(), entry.getValue());
        }
    }
}

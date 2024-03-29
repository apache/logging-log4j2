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
package org.apache.logging.log4j.kit.env.internal;

import java.util.Collections;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.support.CompositePropertyEnvironment;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class ContextualEnvironmentPropertySourceTest extends AbstractPropertyNamesTest {
    @Test
    @SetEnvironmentVariable(key = "LOG4J_CONTEXTS_FOO_MESSAGE_FACTORY", value = "env")
    @SetEnvironmentVariable(key = "LOG4J_CONTEXTS_FOO_TRANSPORT_SECURITY_KEY_STORE_PATH", value = "env")
    void environment_variables_are_recognized() {
        final PropertyEnvironment environment = new CompositePropertyEnvironment(
                null, Collections.singleton(new ContextualEnvironmentPropertySource("foo", 0)), LOADER, LOGGER);
        assertPropertiesAreSet("env", environment);
    }
}

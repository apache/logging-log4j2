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

import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DisabledUntil;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

class PropertiesUtilPropertyEnvironmentTest extends AbstractPropertyNamesTest {

    @Test
    @SetEnvironmentVariable(key = "LOG4J_MESSAGE_FACTORY", value = "env")
    @SetEnvironmentVariable(key = "LOG4J_TRANSPORT_SECURITY_KEY_STORE_PATH", value = "env")
    @DisabledUntil(date = "2024-04-01")
    void environment_variables_are_recognized() {
        final PropertyEnvironment environment = new PropertiesUtilPropertyEnvironment(PROPERTIES_UTIL, LOGGER);
        assertPropertiesAreSet("env", environment);
    }

    @Test
    @SetSystemProperty(key = "log4j2.messageFactory", value = "2.x")
    @SetSystemProperty(key = "log4j2.transportSecurityKeyStorePath", value = "2.x")
    void properties_2_x_are_recognized() {
        final PropertyEnvironment environment = new PropertiesUtilPropertyEnvironment(PROPERTIES_UTIL, LOGGER);
        assertPropertiesAreSet("2.x", environment);
    }

    @Test
    @SetSystemProperty(key = "log4j.message.factory", value = "3.x")
    @SetSystemProperty(key = "log4j.transportSecurity.keyStore.path", value = "3.x")
    void properties_3_x_are_recognized() {
        final PropertyEnvironment environment = new PropertiesUtilPropertyEnvironment(PROPERTIES_UTIL, LOGGER);
        assertPropertiesAreSet("3.x", environment);
    }

    @Test
    @SetTestProperty(key = "log4j2.fooBar", value = "test")
    @SetTestProperty(key = "log4j2.baz", value = "test")
    void legacy_properties_are_recognized() {
        final PropertyEnvironment environment = new PropertiesUtilPropertyEnvironment(PROPERTIES_UTIL, LOGGER);
        assertPropertiesAreSet("test", environment);
    }
}

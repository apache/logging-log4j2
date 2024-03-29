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

import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.assertj.core.api.Assertions;

public abstract class AbstractPropertyNamesTest {

    // Prevents values in tests cases from being cached
    protected static final PropertiesUtil PROPERTIES_UTIL = PropertiesUtil.getProperties();
    protected static final ClassLoader LOADER = AbstractPropertyNamesTest.class.getClassLoader();
    protected static final Logger LOGGER = StatusLogger.getLogger();

    protected Stream<String> testPropertyNames() {
        return Stream.of("Message.factory", "TransportSecurity.keyStore.path");
    }

    protected void assertPropertiesAreSet(final String expected, final PropertyEnvironment environment) {
        Assertions.assertThat(testPropertyNames())
                .extracting(environment::getStringProperty)
                .isSubsetOf(expected);
    }
}

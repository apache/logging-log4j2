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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class LevelMixInXmlTest extends LevelMixInTest {

    @Override
    protected ObjectMapper newObjectMapper() {
        return new Log4jXmlObjectMapper();
    }

    @Test
    @Disabled("String-like objects like Level do not work as root elements.")
    @Override
    public void testNameOnly() throws IOException {
        // Disabled: see https://github.com/FasterXML/jackson-dataformat-xml
        super.testNameOnly();
    }
}

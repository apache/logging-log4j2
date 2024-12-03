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

import static org.apache.logging.log4j.test.SerializableMatchers.serializesRoundTrip;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@LoggerContextSource(value = "log4j-test1.xml")
public class SerializeTest {

    Logger logger = LoggerFactory.getLogger("LoggerTest");

    @Test
    public void testLogger() throws Exception {
        assertThat((Serializable) logger, serializesRoundTrip());
    }
}

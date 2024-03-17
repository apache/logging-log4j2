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
package org.apache.logging.log4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class ScopedContextTest {

    @Test
    public void testScope() {
        ScopedContext.newInstance()
                .where("key1", "Log4j2")
                .run(() -> assertThat(ScopedContext.get("key1"), equalTo("Log4j2")));
        ScopedContext.newInstance().where("key1", "value1").run(() -> {
            assertThat(ScopedContext.get("key1"), equalTo("value1"));
            ScopedContext.newInstance(true).where("key2", "value2").run(() -> {
                assertThat(ScopedContext.get("key1"), equalTo("value1"));
                assertThat(ScopedContext.get("key2"), equalTo("value2"));
            });
            ScopedContext.newInstance().where("key2", "value2").run(() -> {
                assertThat(ScopedContext.get("key1"), nullValue());
                assertThat(ScopedContext.get("key2"), equalTo("value2"));
            });
        });
    }
}

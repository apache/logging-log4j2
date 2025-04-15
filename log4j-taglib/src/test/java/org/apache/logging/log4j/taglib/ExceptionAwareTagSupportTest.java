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
package org.apache.logging.log4j.taglib;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
class ExceptionAwareTagSupportTest {
    private ExceptionAwareTagSupport tag;

    @BeforeEach
    void setUp() {
        this.tag = new ExceptionAwareTagSupport() {
            private static final long serialVersionUID = 1L;
        };
    }

    @Test
    void testException() {
        assertNull(this.tag.getException(), "The exception should be null (1).");

        Exception e = new Exception();
        this.tag.setException(e);
        assertSame(e, this.tag.getException(), "The exception is not correct (1).");

        this.tag.init();
        assertNull(this.tag.getException(), "The exception should be null (2).");

        e = new RuntimeException();
        this.tag.setException(e);
        assertSame(e, this.tag.getException(), "The exception is not correct (2).");
    }
}

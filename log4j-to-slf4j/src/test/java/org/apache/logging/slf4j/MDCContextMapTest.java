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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.spi.ThreadContextMap;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;
import org.slf4j.MDCTestHelper;
import org.slf4j.spi.MDCAdapter;

class MDCContextMapTest {

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/1426")
    void nonNullGetCopy() {
        final ThreadContextMap contextMap = new MDCContextMap();
        final MDCAdapter mockAdapter = mock(MDCAdapter.class);
        when(mockAdapter.getCopyOfContextMap()).thenReturn(null);
        final MDCAdapter adapter = MDCTestHelper.replaceMDCAdapter(mockAdapter);
        try {
            assertThat(contextMap.getImmutableMapOrNull()).isNull();
            assertThat(contextMap.getCopy()).isNotNull();
            verify(mockAdapter, times(2)).getCopyOfContextMap();
            verifyNoMoreInteractions(mockAdapter);
        } finally {
            MDCTestHelper.replaceMDCAdapter(adapter);
        }
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTemplateLayoutTimestampTest
{
    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    /**
     * Tests LOG4J2-3087 race when creating layout on the same instant as the log event would result in an unquoted date in the JSON
     */
    @RepeatedTest( 20 )
    void test_timestamp_pattern_race() {
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate("{\"t\":{\"$resolver\":\"timestamp\",\"pattern\":{\"format\":\"yyyy-MM-dd\"}}}")
                .build();

        final LogEvent logEvent = LogEventFixture.createLiteLogEvents(1).get(0);
        final String json = layout.toSerializable(logEvent);
        assertThat(json).matches("\\{\"t\":\"\\d{4}-\\d{2}-\\d{2}\"}\\R*");
    }
}

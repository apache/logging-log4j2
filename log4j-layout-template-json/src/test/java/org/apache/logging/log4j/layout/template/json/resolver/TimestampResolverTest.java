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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.LogEventFixture;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampResolverTest
{
    private static final TemplateResolverConfig TEMPLATE_RESOLVER_CONFIG = new TemplateResolverConfig(
            Collections.singletonMap("pattern", Collections.singletonMap("format", "yyyy-MM-dd")));

    /**
     * Tests LOG4J2-3087 race when creating layout on the same instant as the log event would result in an unquoted date in the JSON
     */
    @RepeatedTest( 20 )
    void test_timestamp_pattern_race() {
        JsonWriter jsonWriter = JsonWriter.newBuilder()
                .setMaxStringLength(32)
                .setTruncatedStringSuffix("…")
                .build();

        final TimestampResolver resolver = new TimestampResolver(TEMPLATE_RESOLVER_CONFIG);

        final LogEvent logEvent = LogEventFixture.createLiteLogEvents(1).get(0);

        resolver.resolve(logEvent, jsonWriter);

        assertThat(jsonWriter.getStringBuilder().toString()).matches("\"\\d{4}-\\d{2}-\\d{2}\"");
    }
}

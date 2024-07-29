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
package org.apache.logging.log4j.layout.template.json.fuzz;

import static org.apache.logging.log4j.layout.template.json.fuzz.JsonUtil.assertValidJson;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

public final class JsonTemplateLayoutCodecFuzzer {

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final Object object = decodeJson(dataProvider);
        encodeJson(object);
    }

    private static Object decodeJson(final FuzzedDataProvider dataProvider) {
        final String expectedJson = dataProvider.consumeRemainingAsString();
        try {
            return JsonReader.read(expectedJson);
        } catch (final Exception ignored) {
            // We are inspecting unexpected access.
            // Hence, event decoding failures are not of interest.
            return null;
        }
    }

    private static void encodeJson(final Object object) {
        final JsonWriter jsonWriter = JsonWriter.newBuilder()
                .setMaxStringLength(JsonTemplateLayoutDefaults.getMaxStringLength())
                .setTruncatedStringSuffix(JsonTemplateLayoutDefaults.getTruncatedStringSuffix())
                .build();
        jsonWriter.writeValue(object);
        final String actualJson = jsonWriter.getStringBuilder().toString();
        assertValidJson(actualJson);
    }
}

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

import static org.apache.logging.log4j.fuzz.JsonUtil.assertValidJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

// FUZZER-SETUP-SCRIPT cp ${outputDir}/json.dict ${scriptPath}.dict
// FUZZER-SETUP-SCRIPT cp ${outputDir}/json_seed_corpus.zip ${scriptPath}_seed_corpus.zip

public final class JsonTemplateLayoutCodecFuzzer {

    // Choosing a reasonable JSON document length sufficing following needs:
    //
    // 1. Long enough to give fuzzer room to try breaking stuff
    //
    // 2. Short enough to avoid stack overflows, which are interpreted as fuzzer failures, though they are not.
    //    Consider a JSON document containing more than 1024 nested objects.
    //    This triggers a stack overflow, which is expected, hence nothing to signal.
    public static final int MAX_STRING_LENGTH = 512;

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final boolean encodeFirst = dataProvider.consumeBoolean();
        if (encodeFirst) {
            encodeThenDecode(dataProvider);
        } else {
            decodeThenEncode(dataProvider);
        }
    }

    private static void encodeThenDecode(final FuzzedDataProvider dataProvider) {
        final String expectedString = dataProvider.consumeString(MAX_STRING_LENGTH);
        final String json = encodeJson(expectedString);
        final Object actualString = JsonReader.read(json);
        assertThat(actualString).isEqualTo(expectedString);
    }

    private static void decodeThenEncode(final FuzzedDataProvider dataProvider) {
        final Object object = decodeJson(dataProvider);
        final String json = encodeJson(object);
        assertValidJson(json);
    }

    private static Object decodeJson(final FuzzedDataProvider dataProvider) {
        final String expectedJson = dataProvider.consumeString(MAX_STRING_LENGTH);
        try {
            return JsonReader.read(expectedJson);
        } catch (final Exception ignored) {
            // We are inspecting unexpected access.
            // Hence, event decoding failures are not of interest.
            return null;
        }
    }

    private static String encodeJson(final Object object) {
        final JsonWriter jsonWriter = JsonWriter.newBuilder()
                .setMaxStringLength(MAX_STRING_LENGTH)
                .setTruncatedStringSuffix(JsonTemplateLayoutDefaults.getTruncatedStringSuffix())
                .build();
        jsonWriter.writeValue(object);
        return jsonWriter.getStringBuilder().toString();
    }
}

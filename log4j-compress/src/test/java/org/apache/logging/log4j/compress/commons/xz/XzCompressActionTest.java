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
package org.apache.logging.log4j.compress.commons.xz;

import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.compress.commons.AbstractCompressActionTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The XZ and LZMA algorithms have additional dependencies and requires a separate Surefire run.
 */
public class XzCompressActionTest extends AbstractCompressActionTest {

    static Stream<String> algorithms() {
        return Stream.of("lzma", "xz");
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void verify_compression_using_Path_params(String algorithm) throws Exception {
        verifyCompressionUsingPathParams(algorithm, Map.of());
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void verify_compression_using_String_params(String algorithm) throws Exception {
        verifyCompressionUsingStringParams(algorithm, Map.of());
    }
}

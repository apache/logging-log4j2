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
package org.apache.logging.log4j.compress.commons.zstd;

import java.util.Map;
import org.apache.logging.log4j.compress.commons.AbstractCompressActionTest;
import org.junit.jupiter.api.Test;

/**
 * The Zstd algorithm has additional dependencies and requires a separate Surefire run.
 */
public class ZstdCompressActionTest extends AbstractCompressActionTest {

    private static final String ALGORITHM = "zstd";

    @Test
    void verify_compression_using_Path_params() throws Exception {
        verifyCompressionUsingPathParams(ALGORITHM, Map.of());
    }

    @Test
    void verify_compression_using_String_params() throws Exception {
        verifyCompressionUsingStringParams(ALGORITHM, Map.of());
    }
}

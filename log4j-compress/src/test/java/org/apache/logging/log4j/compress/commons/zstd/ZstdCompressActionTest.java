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

import static org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory.COMPRESSION_LEVEL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.compress.commons.AbstractCompressActionTest;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Zstd algorithm has additional dependencies and requires a separate Surefire run.
 */
@UsingStatusListener
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

    @ParameterizedTest
    @ValueSource(strings = {"-131072", "-1", "0", "3", "19", "22"})
    void verify_compression_with_level(final String level) throws Exception {
        verifyCompressionUsingPathParams(ALGORITHM, Map.of(COMPRESSION_LEVEL, level));
    }

    /**
     * Zstandard adjusts levels outside of its range instead of rejecting them: the result is the same as with the
     * boundary of the range.
     * <p>
     *     The boundaries are those of zstd-jni 1.5.7.
     * </p>
     */
    @ParameterizedTest
    @CsvSource({"1000000, 22", "-1000000, -131072"})
    void out_of_range_level_is_adjusted_by_zstd(final String level, final String boundary) throws Exception {
        assertThat(compressedBytesOfText(ALGORITHM, Map.of(COMPRESSION_LEVEL, level)))
                .isEqualTo(compressedBytesOfText(ALGORITHM, Map.of(COMPRESSION_LEVEL, boundary)));
        assertThat(statusListener.findStatusData(Level.WARN)).isEmpty();
    }

    @Test
    void zstd_level_is_applied() throws Exception {
        final long fast = compressedSizeOfText(ALGORITHM, Map.of(COMPRESSION_LEVEL, "1"));
        final long best = compressedSizeOfText(ALGORITHM, Map.of(COMPRESSION_LEVEL, "19"));
        assertThat(best).isLessThan(fast);
    }

    @Test
    void non_numeric_level_falls_back_to_default_and_warns() throws Exception {
        verifyCompressionUsingPathParams(ALGORITHM, Map.of(COMPRESSION_LEVEL, "abc"));
        assertThat(statusListener.findStatusData(Level.WARN)).hasSize(1).allSatisfy(data -> assertThat(
                        data.getMessage().getFormattedMessage())
                .contains("compression level 'abc'"));
    }

    private ListStatusListener statusListener;
}

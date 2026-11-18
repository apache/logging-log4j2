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
package org.apache.logging.log4j.core.appender.rolling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the {@code compressionLevel} attribute is passed to the compression action factory.
 */
class RolloverStrategyCompressionLevelTest {

    private static final String COMPRESSION_LEVEL = CompressActionFactory.COMPRESSION_LEVEL;

    private static DefaultRolloverStrategy.Builder defaultStrategy() {
        return new DefaultRolloverStrategy.Builder().setConfig(new NullConfiguration());
    }

    private static DirectWriteRolloverStrategy.Builder directWriteStrategy() {
        return new DirectWriteRolloverStrategy.Builder().setConfig(new NullConfiguration());
    }

    @Test
    void default_strategy_passes_compression_level() {
        assertThat(defaultStrategy().setCompressionLevel(7).build().getCompressOptions())
                .isEqualTo(Map.of(COMPRESSION_LEVEL, "7"));
    }

    @Test
    void default_strategy_passes_no_option_by_default() {
        assertThat(defaultStrategy().build().getCompressOptions()).isEmpty();
    }

    @Test
    void direct_write_strategy_passes_compression_level() {
        assertThat(directWriteStrategy().setCompressionLevel(-1).build().getCompressOptions())
                .isEqualTo(Map.of(COMPRESSION_LEVEL, "-1"));
    }

    @Test
    void direct_write_strategy_passes_no_option_by_default() {
        assertThat(directWriteStrategy().build().getCompressOptions()).isEmpty();
    }
}

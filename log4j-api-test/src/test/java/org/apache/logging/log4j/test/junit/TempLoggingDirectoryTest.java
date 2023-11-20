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
package org.apache.logging.log4j.test.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.test.TestProperties;
import org.junit.jupiter.api.Test;

@UsingTestProperties
public class TempLoggingDirectoryTest {

    private static final Path PER_CLASS_PATH = Paths.get("TempLoggingDirectoryTest");
    private static final Path PER_TEST_PATH = Paths.get("testInjectedFields");

    @TempLoggingDir
    private static Path staticLoggingPath;

    @TempLoggingDir
    private Path instanceLoggingPath;

    @Test
    void testInjectedFields(final @TempLoggingDir Path parameterLoggingPath, final TestProperties props) {
        assertThat(staticLoggingPath).exists().endsWith(PER_CLASS_PATH);
        assertThat(instanceLoggingPath).exists().startsWith(staticLoggingPath).endsWith(PER_TEST_PATH);
        assertThat(parameterLoggingPath).isEqualTo(instanceLoggingPath);
        assertThat(props.getProperty(TestProperties.LOGGING_PATH)).isEqualTo(instanceLoggingPath.toString());
    }
}

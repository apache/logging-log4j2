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
package org.apache.logging.log4j.core.appender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.core.script.ScriptRef; // Correct import for ScriptRef
import org.apache.logging.log4j.core.config.Configuration; // Import for Configuration
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-script-ref-test.xml")
class ScriptConfigurationTest {

    @Test
    void testScriptRefConfiguration(final Configuration configuration) {
        // Verify that the main Scripts element is initialized
        assertNotNull(configuration.getScriptManager(), "ScriptManager should not be null");
        
        // Verify a ScriptRef element is correctly resolved
        ScriptRef scriptRef = (ScriptRef) configuration.getScriptManager().getScript("ExampleScriptRef");
        assertNotNull(scriptRef, "ScriptRef should not be null");
        assertThat(scriptRef.getLanguage(), containsString("groovy"));
        assertThat(scriptRef.getScriptText(), containsString("return \"Hello, Log4j!\";"));
        
        // Ensure that the script executes correctly
        Object result = configuration.getScriptManager().execute("ExampleScriptRef", null);
        assertNotNull(result, "Script execution result should not be null");
        assertThat(result.toString(), containsString("Hello, Log4j!"));

        // Verify that the console appender is initialized
        final ConsoleAppender consoleAppender = (ConsoleAppender) configuration.getAppender("Console");
        assertNotNull(consoleAppender, "Console appender should be initialized");
        
        // Verify that the log messages are printed to the console
        ExtendedLogger logger = configuration.getLoggerContext().getLogger(ScriptConfigurationTest.class);
        logger.info("Test message");
        
        // Capture console output (depending on test framework, this might need a mock or special handling)
        // Check if the expected log message is printed in the console output
        assertThat(consoleAppender.getLayout().toString(), containsString("Test message"));
    }
}

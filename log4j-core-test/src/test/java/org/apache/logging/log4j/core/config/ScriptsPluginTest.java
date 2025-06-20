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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.logging.log4j.core.script.AbstractScript;
import org.junit.jupiter.api.Test;

class ScriptsPluginTest {

    @Test
    void testCreateScriptsNullInput() {
        assertThrows(NullPointerException.class, () -> ScriptsPlugin.createScripts(null));
    }

    @Test
    void testCreateScriptsEmptyInput() {
        AbstractScript[] emptyArray = new AbstractScript[0];
        assertSame(emptyArray, ScriptsPlugin.createScripts(emptyArray), "Should return empty array");
    }

    @Test
    void testCreateScriptsAllExplicitNames() {
        AbstractScript script1 = new MockScript("script1", "JavaScript", "text");
        AbstractScript script2 = new MockScript("script2", "Groovy", "text");
        AbstractScript[] input = {script1, script2};
        AbstractScript[] result = ScriptsPlugin.createScripts(input);
        assertEquals(2, result.length, "Should return 2 scripts");
        assertArrayEquals(input, result, "Should contain all valid scripts");
    }

    @Test
    void testCreateScriptsImplicitName() {
        AbstractScript script = new MockScript(null, "JavaScript", "text");
        AbstractScript[] input = {script};
        assertThrows(ConfigurationException.class, () -> ScriptsPlugin.createScripts(input));
    }

    @Test
    void testCreateScriptsBlankName() {
        AbstractScript script = new MockScript("  ", "JavaScript", "text");
        AbstractScript[] input = {script};
        assertThrows(ConfigurationException.class, () -> ScriptsPlugin.createScripts(input));
    }

    @Test
    void testCreateScriptsMixedExplicitAndImplicitNames() {
        AbstractScript explicitScript = new MockScript("script", "Python", "text");
        AbstractScript implicitScript = new MockScript(null, "JavaScript", "text");
        AbstractScript[] input = {explicitScript, implicitScript};
        assertThrows(ConfigurationException.class, () -> ScriptsPlugin.createScripts(input));
    }

    private class MockScript extends AbstractScript {

        public MockScript(String name, String language, String scriptText) {
            super(name, language, scriptText);
        }
    }
}

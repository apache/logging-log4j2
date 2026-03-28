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
package org.apache.logging.log4j.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AbstractScriptTest {

    @Test
    void testConstructorAndGettersWithExplicitName() {
        final String lang = "JavaScript";
        final String text = "text";
        final String name = "script";
        final AbstractScript script = new MockScript(name, lang, text);

        assertEquals(lang, script.getLanguage(), "Language should match");
        assertEquals(text, script.getScriptText(), "Script text should match");
        assertEquals(name, script.getName(), "Name should match the provided name");
        assertEquals(name, script.getId(), "Id should match the provided name");
    }

    @Test
    void testConstructorAndGettersWithImplicitName() {
        final String lang = "JavaScript";
        final String text = "text";
        final AbstractScript script = new MockScript(null, lang, text);

        assertEquals(lang, script.getLanguage(), "Language should match");
        assertEquals(text, script.getScriptText(), "Script text should match");
        assertNull(script.getName(), "Name should be null");
        assertNotNull(script.getId(), "Id should not be null");
    }

    @Test
    void testConstructorAndGettersWithBlankName() {
        final String lang = "JavaScript";
        final String text = "text";
        final String name = "  ";
        final AbstractScript script = new MockScript(name, lang, text);

        assertEquals(lang, script.getLanguage(), "Language should match");
        assertEquals(text, script.getScriptText(), "Script text should match");
        assertEquals(name, script.getName(), "Name should be blank");
        assertNotNull(script.getId(), "Id should not be null");
    }

    private class MockScript extends AbstractScript {

        public MockScript(String name, String language, String scriptText) {
            super(name, language, scriptText);
        }
    }
}

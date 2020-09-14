/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling.action;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileRenameActionTest {

    @TempDir
    File tempDir;

    @Test
    public void testRename1() throws Exception {
        final File file = new File(tempDir, "fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            for (int i = 0; i < 100; ++i) {
                pos.println("This is line " + i);
            }
        }

        final File dest = new File(tempDir, "newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, false);
        action.execute();
        assertTrue(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file exists");
    }

    @Test
    public void testEmpty() throws Exception {
        final File file = new File(tempDir, "fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            // do nothing
        }

        final File dest = new File(tempDir, "newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, false);
        action.execute();
        assertFalse(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file does not exist");
    }


    @Test
    public void testNoParent() throws Exception {
        final File file = new File("fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            for (int i = 0; i < 100; ++i) {
                pos.println("This is line " + i);
            }
        }

        final File dest = new File("newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, false);
        action.execute();
        assertTrue(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file exists");
    }

}

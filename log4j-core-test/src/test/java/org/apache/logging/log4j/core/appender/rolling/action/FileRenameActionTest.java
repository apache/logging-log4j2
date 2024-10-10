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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

public class FileRenameActionTest {

    static File tempDir = new File("./target");

    @AfterEach
    public void cleanup() {
        final File file = new File(tempDir, "newFile.log");
        file.delete();
    }

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
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertTrue(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file exists");
    }

    @Test
    public void testEmpty() throws Exception {
        final File file = new File(tempDir, "fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            // do nothing
        }
        assertTrue(file.exists(), "File to rename does not exist");
        final File dest = new File(tempDir, "newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, false);
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertFalse(dest.exists(), "Renamed file should not exist");
        assertFalse(file.exists(), "Old file still exists");
    }

    @Test
    public void testRenameEmpty() throws Exception {
        final File file = new File(tempDir, "fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            // do nothing
        }
        assertTrue(file.exists(), "File to rename does not exist");
        final File dest = new File(tempDir, "newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, true);
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertTrue(dest.exists(), "Renamed file should exist");
        assertFalse(file.exists(), "Old file still exists");
    }

    @Test
    public void testNoParent() throws Exception {
        final File file = new File("fileRename.log");
        try (final PrintStream pos = new PrintStream(file)) {
            for (int i = 0; i < 100; ++i) {
                pos.println("This is line " + i);
            }
        }

        final File dest = new File(tempDir, "newFile.log");
        final FileRenameAction action = new FileRenameAction(file, dest, false);
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertTrue(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file exists");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/2592")
    public void testRenameForMissingFile() throws Exception {
        final File file = new File("fileRename.log");
        final File dest = new File(tempDir, "newFile.log");
        FileRenameAction action = new FileRenameAction(file, dest, true);
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertTrue(dest.exists(), "Renamed file does not exist");
        assertFalse(file.exists(), "Old file exists");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/2592")
    public void testRenameForMissingFileWithoutEmptyFilesRenaming() throws Exception {
        final File file = new File("fileRename.log");
        final File dest = new File(tempDir, "newFile.log");
        FileRenameAction action = new FileRenameAction(file, dest, false);
        boolean renameResult = action.execute();
        assertTrue(renameResult, "Rename action returned false");
        assertFalse(dest.exists(), "Renamed file should not exist");
        assertFalse(file.exists(), "Old file exists");
    }
}

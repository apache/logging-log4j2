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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.script.Script;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the ScriptCondition class.
 */
public class ScriptConditionTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullScript() {
        new ScriptCondition(null, new DefaultConfiguration());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorDisallowsNullConfig() {
        new ScriptCondition(new Script("test", "js", "print('hi')"), null);
    }

    @Test
    public void testCreateConditionReturnsNullForNullScript() {
        assertNull(ScriptCondition.createCondition(null, new DefaultConfiguration()));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateConditionDisallowsNullConfig() {
        ScriptCondition.createCondition(new Script("test", "js", "print('hi')"), null);
    }

    @Test
    public void testSelectFilesToDelete() {
        Configuration config = new DefaultConfiguration();
        config.initialize(); // creates the ScriptManager

        Script script = new Script("test", "javascript", "pathList;"); // script that returns pathList
        ScriptCondition condition = new ScriptCondition(script, config);
        List<PathWithAttributes> pathList = new ArrayList<PathWithAttributes>();
        Path base = Paths.get("baseDirectory");
        List<PathWithAttributes> result = condition.selectFilesToDelete(base, pathList);
        assertSame(result, pathList);
    }

    @Test
    public void testSelectFilesToDelete2() {
        Configuration config = new DefaultConfiguration();
        config.initialize(); // creates the ScriptManager

        List<PathWithAttributes> pathList = new ArrayList<PathWithAttributes>();
        pathList.add(new PathWithAttributes(Paths.get("/path/1"), new DummyFileAttributes()));
        pathList.add(new PathWithAttributes(Paths.get("/path/2"), new DummyFileAttributes()));
        pathList.add(new PathWithAttributes(Paths.get("/path/3"), new DummyFileAttributes()));

        String scriptText = "pathList.remove(1);" //
                + "pathList;";
        Script script = new Script("test", "javascript", scriptText);
        ScriptCondition condition = new ScriptCondition(script, config);
        Path base = Paths.get("baseDirectory");
        List<PathWithAttributes> result = condition.selectFilesToDelete(base, pathList);
        assertSame(result, pathList);
        assertEquals(2, result.size());
        assertEquals(Paths.get("/path/1"), result.get(0).getPath());
        assertEquals(Paths.get("/path/3"), result.get(1).getPath());
    }

    @Test
    public void testSelectFilesToDelete3() {
        Configuration config = new DefaultConfiguration();
        config.initialize(); // creates the ScriptManager

        List<PathWithAttributes> pathList = new ArrayList<PathWithAttributes>();
        pathList.add(new PathWithAttributes(Paths.get("/path/1/abc/a.txt"), new DummyFileAttributes()));
        pathList.add(new PathWithAttributes(Paths.get("/path/2/abc/bbb.txt"), new DummyFileAttributes()));
        pathList.add(new PathWithAttributes(Paths.get("/path/3/abc/c.txt"), new DummyFileAttributes()));

        String scriptText = "" //
                + "import java.nio.file.*;" //
                + "def pattern = ~/(\\d*)[\\/\\\\]abc[\\/\\\\].*\\.txt/;" //
                + "assert pattern.getClass() == java.util.regex.Pattern;" //
                + "def copy = pathList.collect{it};"
                + "pathList.each { pathWithAttribs -> \n" //
                + "  def relative = basePath.relativize pathWithAttribs.path;" //
                + "  println 'relative path: ' + relative;" //
                + "  def str = relative.toString();"
                + "  def m = pattern.matcher(str);" //
                + "  if (m.find()) {" //
                + "    def index = m.group(1) as int;" //
                + "    println 'extracted index: ' + index;" //
                + "    def isOdd = (index % 2) == 1;"
                + "    println 'is odd: ' + isOdd;" //
                + "    if (isOdd) { copy.remove pathWithAttribs}"
                + "  }" //
                + "}" //
                + "println copy;"
                + "copy;";
        Script script = new Script("test", "groovy", scriptText);
        ScriptCondition condition = new ScriptCondition(script, config);
        Path base = Paths.get("/path");
        List<PathWithAttributes> result = condition.selectFilesToDelete(base, pathList);
        assertEquals(1, result.size());
        assertEquals(Paths.get("/path/2/abc/bbb.txt"), result.get(0).getPath());
    }

}

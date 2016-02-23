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

package org.apache.logging.log4j.taglib;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TagUtilsLevelTest {

    private final Level level;
    private final String levelName;

    public TagUtilsLevelTest(final Level level, final String levelName) {
        this.level = level;
        this.levelName = levelName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        final Collection<Object[]> params = new ArrayList<>();
        // this is perhaps the laziest way to test all the known levels
        for (final Level level : Level.values()) {
            params.add(new Object[]{level, level.name().toLowerCase()});
        }
        return params;
    }

    @Test
    public void testResolveLevelName() throws Exception {
        assertEquals(level, TagUtils.resolveLevel(levelName));
    }

    @Test
    public void testResolveLevelEnum() throws Exception {
        assertEquals(level, TagUtils.resolveLevel(level));
    }

}

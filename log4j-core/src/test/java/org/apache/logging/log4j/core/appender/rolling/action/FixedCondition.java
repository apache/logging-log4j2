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
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.core.appender.rolling.action.PathCondition;

/**
 * Test helper class.
 */
public class FixedCondition implements PathCondition {

    private final boolean accept;

    public FixedCondition(final boolean accept) {
        this.accept = accept;
    }

    @Override
    public boolean accept(final Path baseDir, final Path path, final BasicFileAttributes attrs) {
        return accept;
    }

    @Override
    public void beforeFileTreeWalk() {
    }
}

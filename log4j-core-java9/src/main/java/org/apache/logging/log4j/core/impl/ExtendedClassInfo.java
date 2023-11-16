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
package org.apache.logging.log4j.core.impl;

import java.io.Serializable;
import org.apache.logging.log4j.core.pattern.TextRenderer;

/**
 * Dummy class to let ExtendedStackTracElement to compile. It will not be copied
 * to `log4j-core`.
 */
public class ExtendedClassInfo implements Serializable {

    public ExtendedClassInfo(final boolean exact, final String location, final String version) {}

    public boolean getExact() {
        return false;
    }

    public String getLocation() {
        return null;
    }

    public String getVersion() {
        return null;
    }

    public void renderOn(final StringBuilder output, final TextRenderer textRenderer) {}
}

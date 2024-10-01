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
package org.apache.log4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineNumberFilter implements Filter {

    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(":\\d+\\)");

    @Override
    public String filter(final String in) {
        Matcher matcher = LINE_NUMBER_PATTERN.matcher(in);
        if (matcher.find()) {
            return matcher.replaceFirst(":XXX");
        }
        return in.replaceFirst(", Compiled Code", ":XXX");
    }
}

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

public interface Filter {

    final String BASIC_PAT = "\\[main\\] (FATAL|ERROR|WARN|INFO|DEBUG)";
    final String ISO8601_PAT = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}";

    // 06 avr. 2002 18:36:32,036
    // 18 fevr. 2002 20:05:36,222
    public static final String ABSOLUTE_DATE_AND_TIME_PAT = "^\\d{1,2} .{2,6}\\.? 2\\d{3} \\d{2}:\\d{2}:\\d{2},\\d{3}";

    // 18:54:19,201
    public static final String ABSOLUTE_TIME_PAT = "^\\d{2}:\\d{2}:\\d{2},\\d{3}";

    public static final String RELATIVE_TIME_PAT = "^\\d{1,10}";

    String filter(String in) throws UnexpectedFormatException;
}

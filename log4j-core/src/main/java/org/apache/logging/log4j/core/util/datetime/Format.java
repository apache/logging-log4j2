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
package org.apache.logging.log4j.core.util.datetime;

import java.text.FieldPosition;

/**
 * The basic methods for performing date formatting.
 *
 * @deprecated Starting with version {@code 2.25.0}, this class is assumed to be internal and planned to be removed in the next major release.
 */
@Deprecated
public abstract class Format {

    public final String format(final Object obj) {
        return format(obj, new StringBuilder(), new FieldPosition(0)).toString();
    }

    public abstract StringBuilder format(Object obj, StringBuilder toAppendTo, FieldPosition pos);
}

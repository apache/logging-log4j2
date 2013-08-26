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
package org.apache.log4j.helpers;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * An always-empty Enumerator.
 *
 * @since version 1.0
 */
@SuppressWarnings("rawtypes")
public final class NullEnumeration implements Enumeration {
    private static final NullEnumeration INSTANCE = new NullEnumeration();

    private NullEnumeration() {
    }

    public static NullEnumeration getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean hasMoreElements() {
        return false;
    }

    @Override
    public Object nextElement() {
        throw new NoSuchElementException();
    }
}

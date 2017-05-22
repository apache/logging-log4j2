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
package org.apache.logging.log4j.util;

import java.util.function.Predicate;

/**
 * <em>This class should be considered to be internal.</em> Used to locate the StackFrame that called the named class.
 */

public final class ClassPredicate implements Predicate<StackWalker.StackFrame> {

    private final Class<?> clazz;

    public ClassPredicate(Class<?> clazz) {
        this.clazz = clazz;
    }

    private boolean next = false;

    @Override
    public boolean test(StackWalker.StackFrame f) {
        if (clazz.equals(f.getDeclaringClass())) {
            next = true;
            return false;
        } else if (next) {
            next = false;
            return true;
        }
        return false;
    }
}

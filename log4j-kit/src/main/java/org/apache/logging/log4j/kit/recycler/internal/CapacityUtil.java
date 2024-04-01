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
package org.apache.logging.log4j.kit.recycler.internal;

public class CapacityUtil {

    private static final int BITS_PER_INT = 32;

    private static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (BITS_PER_INT - Integer.numberOfLeadingZeros(x - 1));
    }

    /**
     * The default recycler capacity: {@code max(2C, 8)}, {@code C} denoting the number of available processors,
     * rounded to the next power of 2.
     */
    public static final int DEFAULT_CAPACITY =
            Math.max(ceilingNextPowerOfTwo(2 * Runtime.getRuntime().availableProcessors()), 8);
}

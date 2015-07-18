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

package org.apache.logging.log4j.core.util;

import java.util.Objects;

/**
 * Creates the appropriate {@link NanoClock} instance for the current configuration.
 */
public class NanoClockFactory {

    /**
     * Enum over the different kinds of nano clocks this factory can create.
     */
    public static enum Mode {
        /**
         * Creates dummy nano clocks that always return a fixed value.
         */
        Dummy {
            public NanoClock createNanoClock() {
                return new DummyNanoClock();
            }
        },
        /**
         * Creates real nano clocks which call {{System.nanoTime()}}.
         */
        System  {
            public NanoClock createNanoClock() {
                return new SystemNanoClock();
            }
        },
        ;
        
        public abstract NanoClock createNanoClock();
    }
    
    private static volatile Mode mode = Mode.Dummy;
    
    /**
     * Returns a new {@code NanoClock} determined by the mode of this factory.
     * 
     * @return the appropriate {@code NanoClock} for the factory mode
     */
    public static NanoClock createNanoClock() {
        return mode.createNanoClock();
    }
    
    /**
     * Returns the factory mode.
     * 
     * @return the factory mode that determines which kind of nano clocks this factory creates
     */
    public static Mode getMode() {
        return mode;
    }
    
    /**
     * Sets the factory mode.
     * 
     * @param mode the factory mode that determines which kind of nano clocks this factory creates
     */
    public static void setMode(Mode mode) {
        NanoClockFactory.mode = Objects.requireNonNull(mode, "mode must be non-null");
    }
}

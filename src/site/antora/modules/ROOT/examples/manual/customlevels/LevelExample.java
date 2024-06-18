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
package example;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LevelExample {

    private static final Logger LOGGER = LogManager.getLogger();

    // tag::custom-definition[]
    // OpenTelemetry additional INFO levels
    private static final Level INFO2 = Level.forName("INFO2", 375);
    private static final Level INFO3 = Level.forName("INFO3", 350);
    private static final Level INFO4 = Level.forName("INFO4", 325);
    // end::custom-definition[]

    public static void main(String[] args) {
        String username = System.getProperty("user.name");
        // tag::standard[]
        LOGGER.log(Level.INFO, "Hello {}!", username);
        LOGGER.atLevel(Level.INFO).log("Hello {}!", username);
        // end::standard[]
        // tag::shorthand[]
        LOGGER.info("Hello {}!", username);
        LOGGER.atInfo().log("Hello {}!", username);
        // end::shorthand[]
        // tag::custom[]
        LOGGER.log(INFO2, "Hello {}!", username);
        LOGGER.atLevel(INFO3).log("Hello {}!", username);
        // end::custom[]
    }
}

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
import org.apache.logging.log4j.util.java9.test.BetterService;
import org.apache.logging.log4j.util.java9.test.Service;
import org.apache.logging.log4j.util.java9.test.Service1;
import org.apache.logging.log4j.util.java9.test.Service2;

open module org.apache.logging.log4j.java9test {
    exports org.apache.logging.log4j.util.java9;

    requires org.apache.logging.log4j;
    requires transitive org.junit.jupiter.engine;
    requires transitive org.junit.jupiter.api;
    requires transitive org.assertj.core;

    uses Service;
    uses BetterService;

    provides Service with Service1, Service2;
    provides BetterService with Service2;
}

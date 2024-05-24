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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class MarkerExample {

    private static final Logger LOGGER = LogManager.getLogger("example.MarkerExample");
    // tag::create-marker[]
    private static final Marker SQL_MARKER = MarkerManager.getMarker("SQL");
    // end::create-marker[]
    // tag::create-marker-parent[]
    private static final Marker QUERY_MARKER =
            MarkerManager.getMarker("SQL_QUERY").addParents(SQL_MARKER);
    private static final Marker UPDATE_MARKER =
            MarkerManager.getMarker("UPDATE").addParents(SQL_MARKER);
    // end::create-marker-parent[]

    public static void main(final String[] args) {
        doQuery("my_table");
        doQueryParent("my_table");
        doUpdate("my_table", "column", "value");
    }

    public static void doQuery(String table) {
        // Do business logic here
        // tag::use-marker[]
        LOGGER.debug(SQL_MARKER, "SELECT * FROM {}", table);
        // end::use-marker[]
    }

    public static void doQueryParent(String table) {
        // Do business logic here
        // tag::use-marker-parent[]
        LOGGER.debug(QUERY_MARKER, "SELECT * FROM {}", table);
        // end::use-marker-parent[]
    }

    public static void doUpdate(String table, String column, String value) {
        // Do business logic here
        // tag::use-marker-parent[]
        LOGGER.debug(UPDATE_MARKER, "UPDATE {} SET {} = {}", table, column, value);
        // end::use-marker-parent[]
    }
}

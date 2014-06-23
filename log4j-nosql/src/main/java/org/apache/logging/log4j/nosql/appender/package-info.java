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
/**
 * The NoSQL Appender supports writing log events to NoSQL databases. The following NoSQL databases are currently
 * supported. You can also easily extend this to support other NoSQL databases by implementing just three interfaces:
 * {@link org.apache.logging.log4j.nosql.appender.NoSqlObject NoSqlObject},
 * {@link org.apache.logging.log4j.nosql.appender.NoSqlConnection NoSqlConnection}, and
 * {@link org.apache.logging.log4j.nosql.appender.NoSqlProvider NoSqlProvider}. You will need the client library for your
 * NoSQL database of choice on the classpath to use this appender; these Maven dependencies are optional and will not
 * automatically be added to your classpath.<br>
 * <br>
 * <ul>
 *     <li><a href="http://www.mongodb.org/" target="_blank">MongoDB</a>: org.mongodb:mongo-java-driver:2.11.1 or newer
 *     must be on the classpath.</li>
 *     <li><a href="http://couchdb.apache.org/" target="_blank">Apache CouchDB</a>: org.lightcouch:lightcouch:0.0.5 or
 *     newer must be on the classpath.</li>
 * </ul>
 */
package org.apache.logging.log4j.nosql.appender;


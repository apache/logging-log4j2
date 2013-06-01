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
 * The JPA Appender supports writing log events to a relational database using the Java Persistence API. You will need
 * a JDBC driver on your classpath for the database you wish to log to. You will also need the Java Persistence API 2.1
 * and your JPA provider of choice on the class path; these Maven dependencies are optional and will not automatically
 * be added to your classpath.
 */
package org.apache.logging.log4j.core.appender.db.jpa;

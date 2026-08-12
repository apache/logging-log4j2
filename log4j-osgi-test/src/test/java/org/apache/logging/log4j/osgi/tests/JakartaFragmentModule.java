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
package org.apache.logging.log4j.osgi.tests;

/**
 * Expected OSGi metadata for Log4j Jakarta fragment bundles attached to {@code log4j-core}.
 */
enum JakartaFragmentModule {

    WEB(
            "org.apache.logging.log4j.jakarta.web",
            "org.apache.logging.log4j.web",
            "org.apache.logging.log4j.web.Log4jServletContainerInitializer"),

    JMS(
            "org.apache.logging.log4j.jakarta.jms",
            "org.apache.logging.log4j.core.appender.mom.jakarta",
            "org.apache.logging.log4j.core.appender.mom.jakarta.JmsManager"),

    SMTP(
            "org.apache.logging.log4j.jakarta.smtp",
            "org.apache.logging.log4j.smtp",
            "org.apache.logging.log4j.smtp.SmtpManager"),

    JPA(
            "org.apache.logging.log4j.jakarta.jpa",
            "org.apache.logging.log4j.core.appender.db.jpa",
            "org.apache.logging.log4j.core.appender.db.jpa.converter.LevelAttributeConverter");

    private static final String CORE_FRAGMENT_HOST = "org.apache.logging.log4j.core";

    private final String symbolicName;
    private final String exportPackagePrefix;
    private final String sampleClassName;

    JakartaFragmentModule(
            final String symbolicName, final String exportPackagePrefix, final String sampleClassName) {
        this.symbolicName = symbolicName;
        this.exportPackagePrefix = exportPackagePrefix;
        this.sampleClassName = sampleClassName;
    }

    String symbolicName() {
        return symbolicName;
    }

    String exportPackagePrefix() {
        return exportPackagePrefix;
    }

    String sampleClassName() {
        return sampleClassName;
    }

    String fragmentHost() {
        return CORE_FRAGMENT_HOST;
    }
}

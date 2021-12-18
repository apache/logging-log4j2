<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Apache Log4j 2.17.0 Release Notes

The Apache Log4j 2 team is pleased to announce the Log4j 2.17.0 release!

Apache Log4j is a well known framework for logging application behavior. Log4j 2 is an upgrade
to Log4j that provides significant improvements over its predecessor, Log4j 1.x, and provides
many other modern features such as support for Markers, lambda expressions for lazy logging,
property substitution using Lookups, multiple patterns on a PatternLayout and asynchronous
Loggers. Another notable Log4j 2 feature is the ability to be "garbage-free" (avoid allocating
temporary objects) while logging. In addition, Log4j 2 will not lose events while reconfiguring.

The artifacts may be downloaded from https://logging.apache.org/log4j/2.x/download.html.

This release contains the changes noted below:

* Address CVE-2021-45105.
* Require components that use JNDI to be enabled individually via system properties.
* Remove LDAP and LDAPS as supported protocols from JNDI.

Due to a break in compatibility in the SLF4J binding, Log4j now ships with two versions of the SLF4J to Log4j adapters.
log4j-slf4j-impl should be used with SLF4J 1.7.x and earlier and log4j-slf4j18-impl should be used with SLF4J 1.8.x and
later. SLF4J-2.0.0 alpha releases are not fully supported. See https://issues.apache.org/jira/browse/LOG4J2-2975 and
https://jira.qos.ch/browse/SLF4J-511.

Some of the changes in Log4j 2.17.0 include:

* Disable recursive evaluation of Lookups during log event processing. Recursive evaluation is still allwoed while
generating the configuration.
* The JndiLookup, JndiContextSelector, and JMSAppender now require individual system properties to be enabled.
* Removed support for the LDAP and LDAPS protocols via JNDI.

The Log4j 2.17.0 API, as well as many core components, maintains binary compatibility with previous releases.

## GA Release 2.17.0

Changes in this version include:


### Fixed Bugs
                * [LOG4J2-3230](https://issues.apache.org/jira/browse/LOG4J2-3230):
    Fix string substitution recursion.
                * [LOG4J2-3242](https://issues.apache.org/jira/browse/LOG4J2-3242):
    Limit JNDI to the java protocol only. JNDI will remain disabled by default. Rename JNDI enablement property from
        'log4j2.enableJndi' to 'log4j2.enableJndiLookup', 'log4j2.enableJndiJms', and 'log4j2.enableJndiContextSelector'.
                * [LOG4J2-3242](https://issues.apache.org/jira/browse/LOG4J2-3242):
    Limit JNDI to the java protocol only. JNDI will remain disabled by default. The enablement
        property has been renamed to 'log4j2.enableJndiJava'
                * [LOG4J2-3241](https://issues.apache.org/jira/browse/LOG4J2-3241):
    Do not declare log4j-api-java9 and log4j-core-java9 as dependencies as it causes problems with the
        Maven enforcer plugin.
                * [LOG4J2-3247](https://issues.apache.org/jira/browse/LOG4J2-3247):
    PropertiesConfiguration.parseAppenderFilters NPE when parsing properties file filters.
                * [LOG4J2-3249](https://issues.apache.org/jira/browse/LOG4J2-3249):
    Log4j 1.2 bridge for Syslog Appender defaults to port 512 instead of 514.
                * [LOG4J2-3237](https://issues.apache.org/jira/browse/LOG4J2-3237):
    Log4j 1.2 bridge API hard codes the Syslog protocol to TCP.


---

Apache Log4j 2.17.0 requires a minimum of Java 8 to build and run. Log4j 2.12.1 is the last release to support
Java 7. Java 7 is not longer supported by the Log4j team.

For complete information on Apache Log4j 2, including instructions on how to submit bug
reports, patches, or suggestions for improvement, see the Apache Apache Log4j 2 website:

https://logging.apache.org/log4j/2.x/


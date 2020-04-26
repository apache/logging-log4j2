<!-- vim: set syn=markdown : -->
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Log4j Security Vulnerabilities

This page lists all the security vulnerabilities fixed in released versions of Apache Log4j 2.
Each vulnerability is given a [security impact rating](#Security_Impact_Levels) 
by the [Apache Logging security team](mailto:private@logging.apache.org). 
please note that this rating may vary from platform to platform. We also list the versions 
of Apache Log4j the flaw is known to affect, and where a flaw has not been verified list 
the version with a question mark.

Note: Vulnerabilities that are not Log4j vulnerabilities but have either been incorrectly 
reported against Log4j or where Log4j provides a workaround are listed at the end of this page.

Please note that Log4j 1.x has reached end of life and is no longer supported. Vulnerabilities 
reported after August 2015 against Log4j 1.x were not checked and will not be fixed. Users should 
upgrade to Log4j 2 to obtain security fixes.

Please note that binary patches are never provided. If you need to apply a source code patch, 
use the building instructions for the Apache Log4j version that you are using. For 
Log4j 2 this is BUILDING.md. This file can be found in the 
root subdirectory of a source distributive. 

If you need help on building or configuring Log4j or other help on following the instructions 
to mitigate the known vulnerabilities listed here, please send your questions to the public 
Log4j Users mailing list

If you have encountered an unlisted security vulnerability or other unexpected behaviour 
that has security impact, or if the descriptions here are incomplete, please report them 
privately to the [Log4j Security Team](mailto:private@logging.apache.org). Thank you.

### Fixed in Log4j 2.13.2

[CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488):  Improper validation of certificate with host mismatch in Apache Log4j SMTP appender.

Severity: Low

CVSS Base Score: 3.7 (Low) CVSS:3.0/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N

Versions Affected: all versions from 2.0-alpha1 to 2.13.1

Descripton: Improper validation of certificate with host mismatch in
Log4j2 SMTP appender. This could allow an SMTPS connection to be
intercepted by a man-in-the-middle attack which could leak any log
messages sent through that appender. 

The reported issue was caused by an error in SslConfiguration. Any element using SslConfiguration
in the Log4j Configuration is also affected by this issue. This includes HttpAppender,
SocketAppender, and SyslogAppender. Usages of SslConfiguration that are configured via system
properties are not affected.

Mitigation: Users should upgrade to Apache Log4j 2.13.2 which fixed
this issue in LOG4J2-2819 by making SSL settings configurable for
SMTPS mail sessions. As a workaround for previous releases, users can
set the `mail.smtp.ssl.checkserveridentity` system property to `true`
to enable SMTPS hostname verification for all SMTPS mail sessions.

Credit: This issues was discovered by Peter Stöckli.

References: https://issues.apache.org/jira/browse/LOG4J2-2819
 
### Fixed in Log4j 2.8.2

[CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645): Apache Log4j socket receiver deserialization vulnerability.
 
Severity: Moderate
 
CVSS Base Score: 7.5 (AV:N/AC:L/Au:N/C:P/I:P/A:P)
 
Versions Affected: all versions from 2.0-alpha1 to 2.8.1
 
Description: When using the TCP socket server or UDP socket server to
receive serialized log events from another application, a specially crafted
binary payload can be sent that, when deserialized, can execute arbitrary
code.
 
Mitigation: Java 7+ users should migrate to version 2.8.2 or avoid using
the socket server classes. Java 6 users should avoid using the TCP or UDP
socket server classes, or they can manually backport the security fix from
2.8.2: https://github.com/apache/logging-log4j2/commit/5dcc192
 
Credit: This issue was discovered by Marcio Almeida de Macedo of Red Team
at Telstra
 
References: <https://issues.apache.org/jira/browse/LOG4J2-1863>
 
<a name="Security_Impact_Levels"></a>
## Summary of security impact levels for Apache Log4j
The Apache Log4j Security Team rates the impact of each security flaw that affects Log4j. 
We've chosen a rating scale quite similar to those used by other major vendors in order to 
be consistent. Basically the goal of the rating system is to answer the question "How worried 
should I be about this vulnerability?".
   
Note that the rating chosen for each flaw is the worst possible case across all architectures. 
To determine the exact impact of a particular vulnerability on your own systems you will still 
need to read the security advisories to find out more about the flaw.
   
We use the following descriptions to decide on the impact rating to give each vulnerability:
   
### Critical
A vulnerability rated with a Critical impact is one which could potentially be exploited by 
a remote attacker to get Log4j to execute arbitrary code (either as the user the server is 
running as, or root). These are the sorts of vulnerabilities that could be exploited automatically 
by worms.
   
### Important
A vulnerability rated as Important impact is one which could result in the compromise of data 
or availability of the server. For Log4j this includes issues that allow an easy remote denial 
of service (something that is out of proportion to the attack or with a lasting consequence), 
access to arbitrary files outside of the context root, or access to files that should be otherwise 
prevented by limits or authentication.
   
### Moderate
A vulnerability is likely to be rated as Moderate if there is significant mitigation to make the 
issue less of an impact. This might be because the flaw does not affect likely configurations, or 
it is a configuration that isn't widely used. 
   
### Low
All other security flaws are classed as a Low impact. This rating is used for issues that are believed 
to be extremely hard to exploit, or where an exploit gives minimal consequences.
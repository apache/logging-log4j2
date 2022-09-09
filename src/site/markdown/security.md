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
by the [Apache Logging security team](mailto:security@logging.apache.org).
Note that this rating may vary from platform to platform. We also list the versions
of Apache Log4j the flaw is known to affect, and where a flaw has not been verified list
the version with a question mark.

[Log4j 1.x](http://logging.apache.org/log4j/1.2/) has 
[reached End of Life](https://blogs.apache.org/foundation/entry/apache_logging_services_project_announces)
in 2015 and is no longer supported.
Vulnerabilities reported after August 2015 against Log4j 1.x were not checked and will not be fixed.
Users should [upgrade to Log4j 2](manual/migration.html) to obtain security fixes.

Binary patches are never provided. If you need to apply a source code patch,
use the building instructions for the Apache Log4j version that you are using.
For Log4j 2 these can be found in `BUILDING.md` located in the root subdirectory of the source distribution.

If you need help on building or configuring Log4j or other help on following the instructions
to mitigate the known vulnerabilities listed here, please
[subscribe to](mailto:log4j-user-subscribe@logging.apache.org), and send your questions to the public
Log4j [Users mailing list](mail-lists.html).

If you have encountered an unlisted security vulnerability or other unexpected behaviour
that has security impact, or if the descriptions here are incomplete, please report them
privately to the [Log4j Security Team](mailto:security@logging.apache.org). Thank you!

<a name="CVE-2021-44832"/><a name="cve-2021-44832"/>
## <a name="log4j-2.17.1"/> Fixed in Log4j 2.17.1 (Java 8), 2.12.4 (Java 7) and 2.3.2 (Java 6)

[CVE-2021-44832](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832):
Apache Log4j2 vulnerable to RCE via JDBC Appender when attacker controls configuration.

| [CVE-2021-44832](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832) | Remote Code Execution |
| ---------------   | -------- |
| Severity          | Moderate |
| Base CVSS Score   | 6.6 (AV:N/AC:H/PR:H/UI:N/S:U/C:H/I:H/A:H) |
| Versions Affected | All versions from 2.0-beta7 to 2.17.0, excluding 2.3.2 and 2.12.4 |

### Description
Apache Log4j2 versions 2.0-beta7 through 2.17.0 (excluding security fix releases 2.3.2 and 2.12.4) are vulnerable to
a remote code execution (RCE) attack where an attacker with permission to modify the logging configuration file can
construct a malicious configuration using a JDBC Appender with a data source referencing a JNDI URI which can execute
remote code. This issue is fixed by limiting JNDI data source names to the java protocol in Log4j2 versions 2.17.1,
2.12.4, and 2.3.2.


### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x is not impacted by this vulnerability.

#### Log4j 2.x mitigation

Upgrade to Log4j 2.3.2 (for Java 6), 2.12.4 (for Java 7), or 2.17.1 (for Java 8 and later).

In prior releases confirm that if the JDBC Appender is being used it is not configured to use any protocol
other than Java.

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

Also note that Apache Log4j is the only Logging Services subproject affected by this vulnerability.
Other projects like Log4net and Log4cxx are not impacted by this.

### Release Details
From version 2.17.1, (and 2.12.4 and 2.3.2 for Java 7 and Java 6),
the JDBC Appender will use JndiManager and will require the `log4j2.enableJndiJdbc` system property to contain
a value of true for JNDI to be enabled.

The property to enable JNDI has been renamed from 'log4j2.enableJndi'
to three separate properties: `log4j2.enableJndiLookup`, `log4j2.enableJndiJms`, and `log4j2.enableJndiContextSelector`.

JNDI functionality has been hardened in these versions: 2.3.1, 2.12.2, 2.12.3 or 2.17.0:
from these versions onwards, support for the LDAP protocol has been removed and only the JAVA protocol is supported in JNDI connections.


### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
No credit is being awarded for this issue.

### References
- [CVE-2021-44832](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832)

<a name="CVE-2021-45105"/><a name="cve-2021-45046"/>
## <a name="log4j-2.17.0"/> Fixed in Log4j 2.17.0 (Java 8), 2.12.3 (Java 7) and 2.3.1 (Java 6)

[CVE-2021-45105](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45105):
Apache Log4j2 does not always protect from infinite recursion in lookup evaluation

| [CVE-2021-45105](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45105) | Denial of Service |
| ---------------   | -------- |
| Severity          | Moderate |
| Base CVSS Score   | 5.9 (AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H) |
| Versions Affected | All versions from 2.0-alpha1 to 2.16.0, excluding 2.12.3 |

### Description
Apache Log4j2 versions 2.0-alpha1 through 2.16.0, excluding 2.12.3, did not protect from uncontrolled recursion from self-referential lookups.
When the logging configuration uses a non-default Pattern Layout with a Context Lookup (for example, ``$${ctx:loginId}``),
attackers with control over Thread Context Map (MDC) input data can craft malicious input data that contains a recursive lookup,
resulting in a StackOverflowError that will terminate the process. This is also known as a DOS (Denial of Service) attack.


### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x is not impacted by this vulnerability.

#### Log4j 2.x mitigation

Upgrade to Log4j 2.3.1 (for Java 6), 2.12.3 (for Java 7), or 2.17.0 (for Java 8 and later).

Alternatively, this infinite recursion issue can be mitigated in configuration:

* In PatternLayout in the logging configuration, replace Context Lookups like `${ctx:loginId}` or `$${ctx:loginId}` with Thread Context Map patterns (%X, %mdc, or %MDC).
* Otherwise, in the configuration, remove references to Context Lookups like `${ctx:loginId}` or `$${ctx:loginId}` where they originate
from sources external to the application such as HTTP headers or user input. Note that this mitigation is insufficient in
releases older than 2.12.2 (Java 7), and 2.16.0 (Java 8 and later) as the issues fixed in those releases will
still be present.

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

Also note that Apache Log4j is the only Logging Services subproject affected by this vulnerability.
Other projects like Log4net and Log4cxx are not impacted by this.

### Release Details
From version 2.17.0, (and 2.12.3 and 2.3.1 for Java 7 and Java 6),
only lookup strings in configuration are expanded recursively;
in any other usage, only the top-level lookup is resolved, and any nested lookups are not resolved.

The property to enable JNDI has been renamed from 'log4j2.enableJndi'
to three separate properties: 'log4j2.enableJndiLookup', 'log4j2.enableJndiJms', and 'log4j2.enableJndiContextSelector'.

JNDI functionality has been hardened in these versions: 2.3.1, 2.12.2, 2.12.3 or 2.17.0:
from these versions onwards, support for the LDAP protocol has been removed and only the JAVA protocol is supported in JNDI connections.


### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
Independently discovered by Hideki Okamoto of Akamai Technologies, Guy Lederfein of Trend Micro Research working with Trend Micro’s Zero Day Initiative, and another anonymous vulnerability researcher.

### References
- [CVE-2021-45105](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45105)
- [LOG4J2-3230](https://issues.apache.org/jira/browse/LOG4J2-3230)





<a name="CVE-2021-45046"/><a name="cve-2021-45046"/>
## <a name="log4j-2.16.0"/> Fixed in Log4j 2.16.0 (Java 8) and Log4j 2.12.2 (Java 7)

[CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046):
Apache Log4j2 Thread Context Lookup Pattern vulnerable to remote code execution in certain non-default configurations

| [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046) | Remote Code Execution |
| ---------------   | -------- |
| Severity          | Critical |
| Base CVSS Score   | 9.0 (AV:N/AC:H/PR:N/UI:N/S:C/C:H/I:H/A:H) |
| Versions Affected | All versions from 2.0-beta9 to 2.15.0, excluding 2.12.2 |

### Description
It was found that the fix to address [CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228) in Apache Log4j 2.15.0 was incomplete in certain non-default configurations.
When the logging configuration uses a non-default Pattern Layout with a Context Lookup (for example, $${ctx:loginId}),
attackers with control over Thread Context Map (MDC) input data can craft malicious input data using a JNDI Lookup pattern,
resulting in an information leak and remote code execution in some environments and local code execution in all environments;
remote code execution has been demonstrated on macOS, Fedora, Arch Linux, and Alpine Linux. 

Note that this vulnerability is not limited to just the JDNI lookup. Any other Lookup could also be included in a 
Thread Context Map variable and possibly have private details exposed to anyone with access to the logs.

### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x is not impacted by this vulnerability.

#### Log4j 2.x mitigation

Implement one of the following mitigation techniques:

* Upgrade to Log4j 2.3.1 (for Java 6), 2.12.3 (for Java 7), or 2.17.0 (for Java 8 and later).
* Otherwise, in any release other than 2.16.0, you may remove the `JndiLookup` class from the classpath: `zip -q -d log4j-core-*.jar org/apache/logging/log4j/core/lookup/JndiLookup.class`

Users are advised that while removing the JndiLookup class prevents a potential RCE from occuring, it still leaves 
the application vulnerable to other misuse of Lookups in Thread Context Map data. While the mitigations listed below in 
the history section help in some situations, the only real solution is to upgarde to one of the releases listed in the
first bullet above (or a newer release).

Users are advised not to enable JNDI in Log4j 2.16.0, since it still allows LDAP connections.
If the JMS Appender is required, use one of these versions: 2.3.1, 2.12.2, 2.12.3 or 2.17.0:
from these versions onwards, only the JAVA protocol is supported in JNDI connections.

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

Also note that Apache Log4j is the only Logging Services subproject affected by this vulnerability.
Other projects like Log4net and Log4cxx are not impacted by this.

### History
**Severity is now Critical**

The original severity of this CVE was rated as Moderate; since this CVE was published security experts found additional
exploits against the Log4j 2.15.0 release, that could lead to information leaks, RCE (remote code execution) and LCE (local code execution) attacks.

Base CVSS Score changed from 3.7 (AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:L) to 9.0 (AV:N/AC:H/PR:N/UI:N/S:C/C:H/I:H/A:H).

The title of this CVE was changed from mentioning Denial of Service attacks to mentioning Remote Code Execution attacks.

Only Pattern Layouts with a Context Lookup (for example, `$${ctx:loginId}`) are vulnerable to this.
This page previously incorrectly mentioned that Thread Context Map pattern (`%X`, `%mdc`, or `%MDC`) in the layout would also allow this vulnerability.

While Log4j 2.15.0 makes a best-effort attempt to restrict JNDI LDAP lookups to localhost by default,
there are ways to bypass this and users should not rely on this.


**Older (discredited) mitigation measures**

This page previously mentioned other mitigation measures, but we discovered that these measures only limit exposure while leaving some attack vectors open.

Other insufficient mitigation measures are: setting system property `log4j2.formatMsgNoLookups` or environment variable `LOG4J_FORMAT_MSG_NO_LOOKUPS` to `true` for releases &gt;= 2.10, or modifying the logging configuration to disable message lookups with `%m{nolookups}`, `%msg{nolookups}` or `%message{nolookups}` for releases &gt;= 2.7 and &lt;= 2.14.1.

The reason these measures are insufficient is that, in addition to the Thread Context
attack vector mentioned above, there are still code paths in Log4j where message lookups could occur:
known examples are applications that use `Logger.printf("%s", userInput)`, or applications that use a custom message factory,
where the resulting messages do not implement `StringBuilderFormattable`. There may be other attack vectors.

The safest thing to do is to upgrade Log4j to a safe version, or remove the `JndiLookup` class from the log4j-core jar.

### Release Details

From version 2.16.0 (for Java 8), the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default.
JNDI lookups in configuration now need to be enabled explicitly.
Users are advised not to enable JNDI in Log4j 2.16.0, since it still allows LDAP connections.
If the JMS Appender is required, use one of these versions: 2.3.1, 2.12.2, 2.12.3 or 2.17.0:
from these versions onwards, only the JAVA protocol is supported in JNDI connections.

From version 2.12.2 (for Java 7) and 2.3.1 (for Java 6), the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default. JNDI lookups in configuration now need to be enabled explicitly.
When enabled, JNDI will only support the JAVA protocol, support for the LDAP protocol has been removed.

From version 2.17.0 (for Java 8), support for the LDAP protocol has been removed and only the JAVA protocol is supported in JNDI connections.

From version 2.17.0 (for Java 8), 2.12.3 (for Java 7) and 2.3.1 (for Java 6),
the property to enable JNDI has been renamed from 'log4j2.enableJndi'
to three separate properties: 'log4j2.enableJndiLookup', 'log4j2.enableJndiJms', and 'log4j2.enableJndiContextSelector'.


### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
This issue was discovered by Kai Mindermann of iC Consult and separately by 4ra1n.

Additional vulnerability details discovered independently by Ash Fox of Google, Alvaro Muñoz and Tony Torralba from GitHub, Anthony Weems of Praetorian, and RyotaK (@ryotkak).

### References
- [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046)
- [LOG4J2-3221](https://issues.apache.org/jira/browse/LOG4J2-3221)


<a name="CVE-2021-44228"/><a name="cve-2021-44228"/>
## <a name="log4j-2.15.0"/> Fixed in Log4j 2.15.0 (Java 8)

[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228):  Apache Log4j2 JNDI
features do not protect against attacker controlled LDAP and other JNDI related endpoints. Log4j2 allows 
Lookup expressions in the data being logged exposing the JNDI vulnerability, as well as other problems, 
to be exploited by end users whose input is being logged.

|[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228) | Remote Code Execution |
| ----------------- | -------- |
| Severity          | Critical |
| Base CVSS Score   | 10.0 CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H |
| Versions Affected | All versions from 2.0-beta9 to 2.14.1 |

### Description
In Apache Log4j2 versions up to and including 2.14.1 (excluding security releases 2.3.1, 2.12.2 and 2.12.3),
the JNDI features used in configurations, log messages, and parameters do not
protect against attacker-controlled LDAP and other JNDI related endpoints.
An attacker who can control log messages or log message parameters can execute
arbitrary code loaded from LDAP servers when message lookup substitution is enabled. 

### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x does not have Lookups so the risk is lower.
Applications using Log4j 1.x are only vulnerable to this attack when they use JNDI in their configuration.
A separate CVE (CVE-2021-4104) has been filed for this vulnerability.
To mitigate: Audit your logging configuration to ensure it has no JMSAppender configured.
Log4j 1.x configurations without JMSAppender are not impacted by this vulnerability.

#### Log4j 2.x mitigation

Implement one of the following mitigation techniques:

* Upgrade to Log4j 2.3.1 (for Java 6), 2.12.3 (for Java 7), or 2.17.0 (for Java 8 and later).
* Otherwise, in any release other than 2.16.0, you may remove the `JndiLookup` class from the classpath: `zip -q -d log4j-core-*.jar org/apache/logging/log4j/core/lookup/JndiLookup.class`

Note that simply removing the JndiLookup only resolves one of the two bugs exposed in CVE-2021-44228. This still 
allows users to enter lookup strings into input fields and cause them to be evaluated, which can cause StackOverflowExceptions
or potentially expose private data to anyone provided access to the logs. While the mitigations listed below in the 
history section help in some situations, the only real solution is to upgarde to one of the releases listed in the 
first bullet above (or a newer release).

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

Also note that Apache Log4j is the only Logging Services subproject affected by this vulnerability.
Other projects like Log4net and Log4cxx are not impacted by this.

### History
#### Older (discredited) mitigation measures

This page previously mentioned other mitigation measures, but we discovered that these measures only limit exposure while leaving some attack vectors open.

The 2.15.0 release was found to still be vulnerable when the configuration has a Pattern
Layout containing a Context Lookup (for example, `$${ctx:loginId}`).
When an attacker can control Thread Context values, they may inject a JNDI Lookup pattern, which will be evaluated and result in a JNDI connection.
While Log4j 2.15.0 makes a best-effort attempt to restrict JNDI connections to localhost by default,
there are ways to bypass this and users should not rely on this.

A new CVE (CVE-2021-45046, see above) was raised for this.

Other insufficient mitigation measures are: setting system property `log4j2.formatMsgNoLookups` or environment variable `LOG4J_FORMAT_MSG_NO_LOOKUPS` to `true` for releases &gt;= 2.10, or modifying the logging configuration to disable message lookups with `%m{nolookups}`, `%msg{nolookups}` or `%message{nolookups}` for releases &gt;= 2.7 and &lt;= 2.14.1.

The reason these measures are insufficient is that, in addition to the Thread Context
attack vector mentioned above, there are still code paths in Log4j where message lookups could occur:
known examples are applications that use `Logger.printf("%s", userInput)`, or applications that use a custom message factory,
where the resulting messages do not implement `StringBuilderFormattable`. There may be other attack vectors.

The safest thing to do is to upgrade Log4j to a safe version, or remove the `JndiLookup` class from the log4j-core jar.

#### Release Details

As of Log4j 2.15.0 the message lookups feature was disabled by default. Lookups in configuration still work.
While Log4j 2.15.0 has an option to enable Lookups in this fashion, users are strongly discouraged from enabling it.
A whitelisting mechanism was introduced for JNDI connections, allowing only localhost by default.
The 2.15.0 release was found to have additional vulnerabilities and is not recommended.

From version 2.16.0 (for Java 8), the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default.
JNDI lookups in configuration now need to be enabled explicitly.
Users are advised not to enable JNDI in Log4j 2.16.0, since it still allows LDAP connections.
If the JMS Appender is required, use one of these versions: 2.3.1, 2.12.2, 2.12.3 or 2.17.0:
from these versions onwards, only the JAVA protocol is supported in JNDI connections.

From version 2.12.2 (for Java 7) and 2.3.1 (for Java 6), the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default. JNDI lookups in configuration now need to be enabled explicitly.
When enabled, JNDI will only support the JAVA protocol, support for the LDAP protocol has been removed.

From version 2.17.0 (for Java 8), support for the LDAP protocol has been removed and only the JAVA protocol is supported in JNDI connections.

From version 2.17.0 (for Java 8), 2.12.3 (for Java 7) and 2.3.1 (for Java 6),
the property to enable JNDI has been renamed from 'log4j2.enableJndi'
to three separate properties: 'log4j2.enableJndiLookup', 'log4j2.enableJndiJms', and 'log4j2.enableJndiContextSelector'.


### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
This issue was discovered by Chen Zhaojun of Alibaba Cloud Security Team.

### References
- [https://issues.apache.org/jira/browse/LOG4J2-3201](https://issues.apache.org/jira/browse/LOG4J2-3201)
- [https://issues.apache.org/jira/browse/LOG4J2-3198](https://issues.apache.org/jira/browse/LOG4J2-3198).

## <a name="log4j-2.13.2"/> Fixed in Log4j 2.13.2 (Java 8) and 2.12.3 (Java 7)
<a name="CVE-2020-9488"/><a name="cve-2020-9488"/>
[CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488):  Improper validation of certificate with host mismatch in Apache Log4j SMTP appender.

| [CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488) | |
| ----------------- | --- |
| Severity          | Low |
| CVSS Base Score   | 3.7 (Low) CVSS:3.0/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:N/A:N |
| Versions Affected | All versions from 2.0-alpha1 to 2.13.1 |

### Description
Improper validation of certificate with host mismatch in
Log4j2 SMTP appender. This could allow an SMTPS connection to be
intercepted by a man-in-the-middle attack which could leak any log
messages sent through that appender.

The reported issue was caused by an error in `SslConfiguration`. Any element using `SslConfiguration`
in the Log4j `Configuration` is also affected by this issue. This includes `HttpAppender`,
`SocketAppender`, and `SyslogAppender`. Usages of `SslConfiguration` that are configured via system
properties are not affected.

### Mitigation
Users should upgrade to Apache Log4j 2.13.2 which fixed this issue in
[https://issues.apache.org/jira/browse/LOG4J2-2819](https://issues.apache.org/jira/browse/LOG4J2-2819)
by making SSL settings configurable for SMTPS mail sessions. As a workaround for previous releases, users can
set the `mail.smtp.ssl.checkserveridentity` system property to `true`
to enable SMTPS hostname verification for all SMTPS mail sessions.

### Credit
This issue was discovered by Peter Stöckli.

### References
- [CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488)
- [LOG4J2-2819](https://issues.apache.org/jira/browse/LOG4J2-2819)


## <a name="log4j-2.8.2"/> Fixed in Log4j 2.8.2 (Java 7)
<a name="CVE-2017-5645"/><a name="cve-2017-5645"/>
[CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645): Apache Log4j socket receiver deserialization vulnerability.

| [CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645) | |
| ----------------- | -------- |
| Severity          | Moderate |
| CVSS Base Score   | 7.5 (AV:N/AC:L/Au:N/C:P/I:P/A:P) |
| Versions Affected | All versions from 2.0-alpha1 to 2.8.1 |

### Description
When using the TCP socket server or UDP socket server to
receive serialized log events from another application, a specially crafted
binary payload can be sent that, when deserialized, can execute arbitrary
code.

### Mitigation
Java 7 and above users should migrate to version 2.8.2 or avoid using
the socket server classes. Java 6 users should avoid using the TCP or UDP
socket server classes, or they can manually backport the
[security fix commit](https://github.com/apache/logging-log4j2/commit/5dcc192) from
2.8.2.

### Credit
This issue was discovered by Marcio Almeida de Macedo of Red Team
at Telstra

### References
- [CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645)
- [LOG4J2-1863](https://issues.apache.org/jira/browse/LOG4J2-1863)
- [Security fix commit](https://github.com/apache/logging-log4j2/commit/5dcc192)

## <a name="Security_Impact_Levels"></a> Summary of security impact levels for Apache Log4j
The Apache Log4j Security Team rates the impact of each security flaw that affects Log4j.
We've chosen a rating scale quite similar to those used by other major vendors in order to
be consistent. Basically the goal of the rating system is to answer the question "How worried
should I be about this vulnerability?".

Note that the rating chosen for each flaw is the worst possible case across all architectures.
To determine the exact impact of a particular vulnerability on your own systems you will still
need to read the security advisories to find out more about the flaw.

We use the following descriptions to decide on the impact rating to give each vulnerability:

| Severity | CVSS v3 Score Range |
| -------- | ------------------- |
| Critical | 9.0 - 10.0          |
| High     | 7.0 - 8.9           |
| Moderate | 4.0 - 6.9           |
| Low      | 0.1 - 3.9           |

### Critical
A vulnerability rated with a Critical impact is one which could potentially be exploited by
a remote attacker to get Log4j to execute arbitrary code (either as the user the server is
running as, or root). These are the sorts of vulnerabilities that could be exploited automatically
by worms. Critical vulnerabilities score between 9.0 and 10.0 on the
[CVSS v3 calculator](https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator).

### High
A vulnerability rated as High impact is one which could result in the compromise of data
or availability of the server. For Log4j this includes issues that allow an easy remote denial
of service (something that is out of proportion to the attack or with a lasting consequence),
access to arbitrary files outside of the context root, or access to files that should be otherwise
prevented by limits or authentication. High vulnerabilities score between 7.0 and 8.9 on the
[CVSS v3 calculator](https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator).

### Moderate
A vulnerability is likely to be rated as Moderate if there is significant mitigation to make the
issue less of an impact. This might be because the flaw does not affect likely configurations, or
it is a configuration that isn't widely used. Moderate vulnerabilities score between 4.0 and 6.9 on the
[CVSS v3 calculator](https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator).

### Low
All other security flaws are classed as a Low impact. This rating is used for issues that are believed
to be extremely hard to exploit, or where an exploit gives minimal consequences. Low vulnerabilities
score between 0.1 and 3.9 on the [CVSS v3 calculator](https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator).

## <a name="cve-creation"></a> CVE creation process

Found security vulnerabilities are subject to voting (by means of [_lazy approval_](https://logging.apache.org/guidelines.html), preferably) in the private [security mailing list](mailto:security@logging.apache.org) before creating a CVE and populating its associated content.
This procedure involves only the creation of CVEs and blocks neither (vulnerability) fixes, nor releases.

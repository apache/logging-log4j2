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


## <a name="log4j-2.16.0"/> Fixed in Log4j 2.16.0 (Java 8)

<a name="CVE-2021-45046"/><a name="cve-2021-45046"/>
[CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046):  Apache Log4j2
Thread Context Message Pattern and Context Lookup Pattern vulnerable to a Denial of Service attack.

| [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046) | Denial of Service |
| ---------------   | -------- |
| Severity          | Moderate |
| Base CVSS Score   | 3.7 (AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:L) |
| Versions Affected | All versions from 2.0-beta9 to 2.15.0 |

### Description
It was found that the fix to address CVE-2021-44228 in Apache Log4j 2.15.0 was incomplete in certain non-default configurations. This could allows attackers with control over Thread Context Map (MDC) input data when the logging configuration uses a non-default Pattern Layout with either a Context Lookup (for example, ``$${ctx:loginId})`` or a Thread Context Map pattern (`%X`, `%mdc`, or `%MDC`) to craft malicious input data using a JNDI Lookup pattern resulting in a denial of service (DOS) attack. Log4j 2.15.0 restricts JNDI LDAP lookups to localhost by default. Note that previous mitigations involving configuration such as to set the system property `log4j2.formatMsgNoLookups` to `true` do NOT mitigate this specific vulnerability.


### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x is not impacted by this vulnerability.

#### Log4j 2.x mitigation

Implement one of the following mitigation techniques:

* Java 8 (or later) users should upgrade to release 2.16.0.
* Java 7 users should upgrade to release 2.12.2.
* Otherwise, remove the `JndiLookup` class from the classpath: `zip -q -d log4j-core-*.jar org/apache/logging/log4j/core/lookup/JndiLookup.class`

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

### History
**Older (discredited) mitigation measures**

This page previously mentioned other mitigation measures, but we discovered that these measures only limit exposure while leaving some attack vectors open.

Other insufficient mitigation measures are: setting system property `log4j2.formatMsgNoLookups` or environment variable `LOG4J_FORMAT_MSG_NO_LOOKUPS` to `true` for releases &gt;= 2.10, or modifying the logging configuration to disable message lookups with `%m{nolookups}`, `%msg{nolookups}` or `%message{nolookups}` for releases &gt;= 2.7 and &lt;= 2.14.1.

The reason these measures are insufficient is that, in addition to the Thread Context
attack vector mentioned above, there are still code paths in Log4j where message lookups could occur:
known examples are applications that use `Logger.printf("%s", userInput)`, or applications that use a custom message factory,
where the resulting messages do not implement `StringBuilderFormattable`. There may be other attack vectors.

The safest thing to do is to upgrade Log4j to a safe version, or remove the `JndiLookup` class from the log4j-core jar.

### Release Details

From version 2.16.0, the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default. JNDI lookups in configuration now need to be enabled explicitly.
Also, Log4j now limits the protocols by default to only `java`, `ldap`, and `ldaps` and limits the ldap
protocols to only accessing Java primitive objects. Hosts other than the local host need to be explicitly allowed.

### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
This issue was discovered by Kai Mindermann of iC Consult.

### References
- [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046)
- [LOG4J2-3221](https://issues.apache.org/jira/browse/LOG4J2-3221)


## <a name="log4j-2.15.0"/> Fixed in Log4j 2.15.0 (Java 8)

<a name="CVE-2021-44228"/><a name="cve-2021-44228"/>
[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228):  Apache Log4j2 JNDI
features do not protect against attacker controlled LDAP and other JNDI related endpoints.

|[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228)||
| ---------------   ||
| Severity          | Critical |
| Base CVSS Score   | 10.0 CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H |
| Versions Affected | All versions from 2.0-beta9 to 2.14.1 |

### Description
In Apache Log4j2 versions up to and including 2.14.1,
the JNDI features used in configurations, log messages, and parameters do not
protect against attacker-controlled LDAP and other JNDI related endpoints. An attacker who can control log
messages or log message parameters can execute arbitrary code loaded from LDAP servers when message lookup
substitution is enabled.

### Mitigation

#### Log4j 1.x mitigation

Log4j 1.x does not have Lookups so the risk is lower.
Applications using Log4j 1.x are only vulnerable to this attack when they use JNDI in their configuration.
A separate CVE (CVE-2021-4104) has been filed for this vulnerability.
To mitigate: Audit your logging configuration to ensure it has no JMSAppender configured.
Log4j 1.x configurations without JMSAppender are not impacted by this vulnerability.

#### Log4j 2.x mitigation

Implement one of the following mitigation techniques:

* Java 8 (or later) users should upgrade to release 2.16.0.
* Java 7 users should upgrade to release 2.12.2.
* Otherwise, remove the `JndiLookup` class from the classpath: `zip -q -d log4j-core-*.jar org/apache/logging/log4j/core/lookup/JndiLookup.class`

Note that only the log4j-core JAR file is impacted by this vulnerability.
Applications using only the log4j-api JAR file without the log4j-core JAR file are not impacted by this vulnerability.

### History
#### Older (discredited) mitigation measures

This page previously mentioned other mitigation measures, but we discovered that these measures only limit exposure while leaving some attack vectors open.

The 2.15.0 release was found to still be vulnerable when the configuration has a pattern
layout containing a Context Lookup (for example, `$${ctx:loginId}`),
or a Thread Context Map pattern `%X`, `%mdc` or `%MDC`.
When an attacker can control Thread Context values, they may inject a JNDI Lookup pattern, which will be evaluated and result in a JNDI connection.
Log4j 2.15.0 restricts JNDI connections to localhost by default, but this may still result in DOS (Denial of Service) attacks, or worse.

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

From version 2.16.0, the message lookups feature has been completely removed. Lookups in configuration still work.
Furthermore, Log4j now disables access to JNDI by default. JNDI lookups in configuration now need to be enabled explicitly.
Also, Log4j now limits the protocols by default to only `java`, `ldap`, and `ldaps` and limits the ldap
protocols to only accessing Java primitive objects. Hosts other than the local host need to be explicitly allowed.

### Work in progress
The Log4j team will continue to actively update this page as more information becomes known.

### Credit
This issue was discovered by Chen Zhaojun of Alibaba Cloud Security Team.

### References
- [https://issues.apache.org/jira/browse/LOG4J2-3201](https://issues.apache.org/jira/browse/LOG4J2-3201)
- [https://issues.apache.org/jira/browse/LOG4J2-3198](https://issues.apache.org/jira/browse/LOG4J2-3198).

## <a name="log4j-2.13.2"/> Fixed in Log4j 2.13.2 (Java 8)
<a name="CVE-2020-9488"/><a name="cve-2020-9488"/>
[CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488):  Improper validation of certificate with host mismatch in Apache Log4j SMTP appender.

| [CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488) | |
| ----------------- | |
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
This issues was discovered by Peter Stöckli.

### References
- [CVE-2020-9488](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-9488)
- [LOG4J2-2819](https://issues.apache.org/jira/browse/LOG4J2-2819)

## <a name="log4j-2.12.2"/> Fixed in Log4j 2.12.2 (Java 7)

<a name="CVE-2021-44228"/><a name="cve-2021-44228"/>
[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228):  Apache Log4j2 JNDI
features do not protect against attacker controlled LDAP and other JNDI related endpoints.

|[CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228)||
| ---------------   ||
| Severity          | Critical |
| Base CVSS Score   | 10.0 CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H |
| Versions Affected | All versions from 2.0-beta9 to 2.14.1 |

See [above](#log4j-2.15.0) for details.

<a name="CVE-2021-45046"/><a name="cve-2021-45046"/>
[CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046):  Apache Log4j2
Thread Context Message Pattern and Context Lookup Pattern vulnerable to a Denial of Service attack.

| [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046) | Denial of Service |
| ---------------   | -------- |
| Severity          | Moderate |
| Base CVSS Score   | 3.7 (AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:L) |
| Versions Affected | All versions from 2.0-beta9 to 2.15.0 |

See [above](#log4j-2.16.0) for details.

### References
- [LOG4J2-3220](https://issues.apache.org/jira/browse/LOG4J2-3220)

## <a name="log4j-2.8.2"/> Fixed in Log4j 2.8.2 (Java 7)
<a name="CVE-2017-5645"/><a name="cve-2017-5645"/>
[CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645): Apache Log4j socket receiver deserialization vulnerability.

| [CVE-2017-5645](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-5645) | |
| ----------------- | |
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

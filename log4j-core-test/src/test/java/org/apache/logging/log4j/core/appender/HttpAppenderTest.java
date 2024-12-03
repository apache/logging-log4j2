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
package org.apache.logging.log4j.core.appender;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslKeyStoreConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/* Fails often on Windows, for example:
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.20.1:test (default-test) on project log4j-core: There are test failures.
[ERROR]
[ERROR] Please refer to C:\vcs\git\apache\logging\logging-log4j2\log4j-core\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date]-jvmRun[N].dump, [date].dumpstream and [date]-jvmRun[N].dumpstream.
[ERROR] ExecutionException The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Command was cmd.exe /X /C ""C:\Program Files\Java\jdk1.8.0_152\jre\bin\java" -Xms256m -Xmx1024m -jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112\surefirebooter1486874613063862199.jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112 2018-01-23T11-03-18_847-jvmRun6 surefire6375637980242546356tmp surefire_441335531705722358735tmp"
[ERROR] Process Exit Code: 0
[ERROR] Crashed tests:
[ERROR] org.apache.logging.log4j.core.appender.HttpAppenderTest
[ERROR] org.apache.maven.surefire.booter.SurefireBooterForkException: ExecutionException The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Command was cmd.exe /X /C ""C:\Program Files\Java\jdk1.8.0_152\jre\bin\java" -Xms256m -Xmx1024m -jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112\surefirebooter1486874613063862199.jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112 2018-01-23T11-03-18_847-jvmRun6 surefire6375637980242546356tmp surefire_441335531705722358735tmp"
[ERROR] Process Exit Code: 0
[ERROR] Crashed tests:
[ERROR] org.apache.logging.log4j.core.appender.HttpAppenderTest
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.awaitResultsDone(ForkStarter.java:496)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.runSuitesForkPerTestSet(ForkStarter.java:443)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.run(ForkStarter.java:295)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.run(ForkStarter.java:246)
[ERROR]         at org.apache.maven.plugin.surefire.AbstractSurefireMojo.executeProvider(AbstractSurefireMojo.java:1124)
[ERROR]         at org.apache.maven.plugin.surefire.AbstractSurefireMojo.executeAfterPreconditionsChecked(AbstractSurefireMojo.java:954)
[ERROR]         at org.apache.maven.plugin.surefire.AbstractSurefireMojo.execute(AbstractSurefireMojo.java:832)
[ERROR]         at org.apache.maven.plugin.DefaultBuildPluginManager.executeMojo(DefaultBuildPluginManager.java:134)
[ERROR]         at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:208)
[ERROR]         at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:154)
[ERROR]         at org.apache.maven.lifecycle.internal.MojoExecutor.execute(MojoExecutor.java:146)
[ERROR]         at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject(LifecycleModuleBuilder.java:117)
[ERROR]         at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject(LifecycleModuleBuilder.java:81)
[ERROR]         at org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder.build(SingleThreadedBuilder.java:51)
[ERROR]         at org.apache.maven.lifecycle.internal.LifecycleStarter.execute(LifecycleStarter.java:128)
[ERROR]         at org.apache.maven.DefaultMaven.doExecute(DefaultMaven.java:309)
[ERROR]         at org.apache.maven.DefaultMaven.doExecute(DefaultMaven.java:194)
[ERROR]         at org.apache.maven.DefaultMaven.execute(DefaultMaven.java:107)
[ERROR]         at org.apache.maven.cli.MavenCli.execute(MavenCli.java:955)
[ERROR]         at org.apache.maven.cli.MavenCli.doMain(MavenCli.java:290)
[ERROR]         at org.apache.maven.cli.MavenCli.main(MavenCli.java:194)
[ERROR]         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
[ERROR]         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
[ERROR]         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
[ERROR]         at java.lang.reflect.Method.invoke(Method.java:498)
[ERROR]         at org.codehaus.plexus.classworlds.launcher.Launcher.launchEnhanced(Launcher.java:289)
[ERROR]         at org.codehaus.plexus.classworlds.launcher.Launcher.launch(Launcher.java:229)
[ERROR]         at org.codehaus.plexus.classworlds.launcher.Launcher.mainWithExitCode(Launcher.java:415)
[ERROR]         at org.codehaus.plexus.classworlds.launcher.Launcher.main(Launcher.java:356)
[ERROR] Caused by: org.apache.maven.surefire.booter.SurefireBooterForkException: The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Command was cmd.exe /X /C ""C:\Program Files\Java\jdk1.8.0_152\jre\bin\java" -Xms256m -Xmx1024m -jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112\surefirebooter1486874613063862199.jar C:\Users\ggregory\AppData\Local\Temp\surefire22320217597495112 2018-01-23T11-03-18_847-jvmRun6 surefire6375637980242546356tmp surefire_441335531705722358735tmp"
[ERROR] Process Exit Code: 0
[ERROR] Crashed tests:
[ERROR] org.apache.logging.log4j.core.appender.HttpAppenderTest
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.fork(ForkStarter.java:686)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.fork(ForkStarter.java:535)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter.access$700(ForkStarter.java:116)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter$2.call(ForkStarter.java:431)
[ERROR]         at org.apache.maven.plugin.surefire.booterclient.ForkStarter$2.call(ForkStarter.java:408)
[ERROR]         at java.util.concurrent.FutureTask.run(FutureTask.java:266)
[ERROR]         at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
[ERROR]         at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
[ERROR]         at java.lang.Thread.run(Thread.java:748)
[ERROR]
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException
[ERROR]
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <goals> -rf :log4j-core
 */
@DisabledOnOs(OS.WINDOWS)
class HttpAppenderTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final String LOG_MESSAGE = "Hello, world!";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(HttpAppenderTest.class.getName())
                .setLoggerFqcn(HttpAppenderTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(LOG_MESSAGE))
                .build();
    }

    private final ResponseDefinitionBuilder SUCCESS_RESPONSE = aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"status\":\"created\"}");

    private final ResponseDefinitionBuilder FAILURE_RESPONSE = aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"status\":\"error\"}");

    private static final JavaLookup JAVA_LOOKUP = new JavaLookup();

    @RegisterExtension
    static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .keystorePath(SslKeyStoreConstants.KEYSTORE_LOCATION)
                    .keystorePassword(String.valueOf(SslKeyStoreConstants.KEYSTORE_PWD()))
                    .keyManagerPassword(String.valueOf(SslKeyStoreConstants.KEYSTORE_PWD()))
                    .keystoreType(SslKeyStoreConstants.KEYSTORE_TYPE))
            .build();

    private static URL wireMockUrl(final String path, final boolean secure, final boolean portMangled)
            throws MalformedURLException {
        final String scheme = secure ? "https" : "http";
        int port = secure ? WIRE_MOCK.getHttpsPort() : WIRE_MOCK.getPort();
        if (portMangled) {
            port++;
        }
        final String url = String.format("%s://localhost:%d%s", scheme, port, path);
        return new URL(url);
    }

    @Test
    void testAppend() throws Exception {
        WIRE_MOCK.stubFor(post(urlEqualTo("/test/log4j/")).willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setUrl(wireMockUrl("/test/log4j/", false, false))
                .build();
        appender.append(createLogEvent());

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
                .withHeader("Host", containing("localhost"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    void testAppendHttps() throws Exception {
        WIRE_MOCK.stubFor(post(urlEqualTo("/test/log4j/")).willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setUrl(wireMockUrl("/test/log4j/", true, false))
                .setSslConfiguration(SslConfiguration.createSSLConfiguration(
                        null,
                        KeyStoreConfiguration.createKeyStoreConfiguration(
                                SslKeyStoreConstants.KEYSTORE_LOCATION,
                                SslKeyStoreConstants.KEYSTORE_PWD(),
                                null,
                                null,
                                SslKeyStoreConstants.KEYSTORE_TYPE,
                                null),
                        TrustStoreConfiguration.createKeyStoreConfiguration(
                                SslKeyStoreConstants.TRUSTSTORE_LOCATION,
                                SslKeyStoreConstants.TRUSTSTORE_PWD(),
                                null,
                                null,
                                SslKeyStoreConstants.TRUSTSTORE_TYPE,
                                null)))
                .setVerifyHostname(false)
                .build();
        appender.append(createLogEvent());

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    void testAppendMethodPut() throws Exception {
        WIRE_MOCK.stubFor(put(urlEqualTo("/test/log4j/1234")).willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setIgnoreExceptions(false)
                .setMethod("PUT")
                .setUrl(wireMockUrl("/test/log4j/1234", false, false))
                .build();
        appender.append(createLogEvent());

        WIRE_MOCK.verify(putRequestedFor(urlEqualTo("/test/log4j/1234"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    void testAppendCustomHeader() throws Exception {
        WIRE_MOCK.stubFor(post(urlEqualTo("/test/log4j/")).willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setIgnoreExceptions(false)
                .setUrl(wireMockUrl("/test/log4j/", false, false))
                .setHeaders(new Property[] {
                    Property.createProperty("X-Test", "header value"),
                    Property.createProperty("X-Runtime", "${java:runtime}")
                })
                .build();
        appender.append(createLogEvent());

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
                .withHeader("X-Test", equalTo("header value"))
                .withHeader("X-Runtime", equalTo(JAVA_LOOKUP.getRuntime()))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    @UsingStatusListener
    void testAppendErrorIgnore(final ListStatusListener statusListener) throws Exception {
        WIRE_MOCK.stubFor(post(urlEqualTo("/test/log4j/")).willReturn(FAILURE_RESPONSE));
        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setUrl(wireMockUrl("/test/log4j/", false, false))
                .build();
        appender.append(createLogEvent());

        WIRE_MOCK.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));

        final List<StatusData> statusDataList = statusListener.getStatusData().collect(Collectors.toList());
        assertThat(statusDataList).anySatisfy(statusData -> {
            assertThat(statusData.getLevel()).isEqualTo(Level.ERROR);
            assertThat(statusData.getFormattedStatus()).contains("Unable to send HTTP in appender [Http]");
        });
    }

    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void testAppendError() throws Exception {
        WIRE_MOCK.stubFor(post(urlEqualTo("/test/log4j/")).willReturn(FAILURE_RESPONSE));
        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setIgnoreExceptions(false)
                .setUrl(wireMockUrl("/test/log4j/", false, false))
                .build();
        final LogEvent logEvent = createLogEvent();
        assertThrows(AppenderLoggingException.class, () -> appender.append(logEvent));
    }

    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    void testAppendConnectError() throws Exception {
        final Appender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(JsonLayout.createDefaultLayout())
                .setConfiguration(CONFIGURATION)
                .setIgnoreExceptions(false)
                .setUrl(wireMockUrl("/test/log4j/", false, true))
                .build();
        final LogEvent logEvent = createLogEvent();
        assertThrows(AppenderLoggingException.class, () -> appender.append(logEvent));
    }
}

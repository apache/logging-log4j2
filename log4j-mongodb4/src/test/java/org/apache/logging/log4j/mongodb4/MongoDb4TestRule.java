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

package org.apache.logging.log4j.mongodb4;

import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Timeout;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * A JUnit test rule to manage a MongoDB embedded instance.
 *
 * TODO Move this class to Apache Commons Testing.
 */
public class MongoDb4TestRule implements TestRule {

    public enum LoggingTarget {
        CONSOLE, NULL;

        public static LoggingTarget getLoggingTarget(final String sysPropertyName, final LoggingTarget defaultValue) {
            return LoggingTarget.valueOf(System.getProperty(sysPropertyName, defaultValue.name()));
        }
    }

    private static final int BUILDER_TIMEOUT_MILLIS = 30000;

    public static int getBuilderTimeoutMillis() {
        return BUILDER_TIMEOUT_MILLIS;
    }

    private static MongodStarter getMongodStarter(final LoggingTarget loggingTarget) {
        if (loggingTarget == null) {
            return MongodStarter.getDefaultInstance();
        }
        switch (loggingTarget) {
        case NULL:
            final Logger logger = LoggerFactory.getLogger(MongoDb4TestRule.class.getName());
            // @formatter:off
            return MongodStarter.getInstance(
                    Defaults
                        .runtimeConfigFor(Command.MongoD, logger)
                        .processOutput(ProcessOutput.getDefaultInstanceSilent()).build());
            // @formatter:on
        case CONSOLE:
            return MongodStarter.getDefaultInstance();
        default:
            throw new NotImplementedException(loggingTarget.toString());
        }
    }

    protected final LoggingTarget loggingTarget;

    protected MongoClient mongoClient;
    protected MongodExecutable mongodExecutable;
    protected MongodProcess mongodProcess;
    protected final String portSystemPropertyName;

    /**
     * Store {@link MongodStarter} (or RuntimeConfig) in a static final field if you
     * want to use artifact store caching (or else disable caching).
     * <p>
     * The test framework {@code de.flapdoodle.embed.mongo} requires Java 8.
     * </p>
     */
    protected final MongodStarter starter;

    /**
     * Constructs a new test rule.
     *
     * @param portSystemPropertyName The system property name for the MongoDB port.
     * @param clazz                  The test case class.
     * @param defaultLoggingTarget   The logging target.
     */
    public MongoDb4TestRule(final String portSystemPropertyName, final Class<?> clazz,
            final LoggingTarget defaultLoggingTarget) {
        this.portSystemPropertyName = Objects.requireNonNull(portSystemPropertyName, "portSystemPropertyName");
        this.loggingTarget = LoggingTarget.getLoggingTarget(clazz.getName() + "." + LoggingTarget.class.getSimpleName(),
                defaultLoggingTarget);
        this.starter = getMongodStarter(this.loggingTarget);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final String value = Objects.requireNonNull(System.getProperty(portSystemPropertyName),
                        "System property '" + portSystemPropertyName + "' is null");
                final int port = Integer.parseInt(value);
                mongodExecutable = starter.prepare(
                // @formatter:off
                        MongodConfig.builder()
                                .version(Version.Main.PRODUCTION)
                                .timeout(new Timeout(BUILDER_TIMEOUT_MILLIS))
                                .net(new Net("localhost", port, Network.localhostIsIPv6())).build());
                // @formatter:on
                mongodProcess = mongodExecutable.start();
                mongoClient = MongoClients.create("mongodb://localhost:" + port);
                try {
                    base.evaluate();
                } finally {
                    if (mongodProcess != null) {
                        mongodProcess.stop();
                        mongodProcess = null;
                    }
                    if (mongodExecutable != null) {
                        mongodExecutable.stop();
                        mongodExecutable = null;
                    }
                }
            }
        };
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongodExecutable getMongodExecutable() {
        return mongodExecutable;
    }

    public MongodProcess getMongodProcess() {
        return mongodProcess;
    }

    public MongodStarter getStarter() {
        return starter;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Mongo4TestRule [starter=");
        builder.append(starter);
        builder.append(", portSystemPropertyName=");
        builder.append(portSystemPropertyName);
        builder.append(", mongoClient=");
        builder.append(mongoClient);
        builder.append(", mongodExecutable=");
        builder.append(mongodExecutable);
        builder.append(", mongodProcess=");
        builder.append(mongodProcess);
        builder.append(", loggingTarget=");
        builder.append(loggingTarget);
        builder.append("]");
        return builder.toString();
    }

}
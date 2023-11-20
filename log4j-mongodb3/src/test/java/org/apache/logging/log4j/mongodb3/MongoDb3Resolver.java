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
package org.apache.logging.log4j.mongodb3;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.PackageOfCommandDistribution;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.mongo.types.DistributionBaseUrl;
import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamProcessor;
import de.flapdoodle.embed.process.types.Name;
import de.flapdoodle.embed.process.types.ProcessConfig;
import de.flapdoodle.os.OSType;
import de.flapdoodle.reverse.TransitionWalker.ReachedState;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import java.util.function.Supplier;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.apache.logging.log4j.test.junit.TestPropertySource;
import org.apache.logging.log4j.test.junit.TypeBasedParameterResolver;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

public class MongoDb3Resolver extends TypeBasedParameterResolver<MongoClient> implements BeforeAllCallback {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String LOGGING_TARGET_PROPERTY = "log4j2.mongoDbLoggingTarget";

    private static final int BUILDER_TIMEOUT_MILLIS = 30000;

    private static ProcessOutput getProcessOutput(final LoggingTarget loggingTarget, final String label) {
        if (loggingTarget != null) {
            switch (loggingTarget) {
                case STATUS_LOGGER:
                    return ProcessOutput.builder()
                            .output(Processors.named(
                                    "[" + label + " output]", new StatusLoggerStreamProcessor(Level.INFO)))
                            .error(Processors.named(
                                    "[" + label + " error]", new StatusLoggerStreamProcessor(Level.ERROR)))
                            .commands(new StatusLoggerStreamProcessor(Level.DEBUG))
                            .build();
                case CONSOLE:
                    return ProcessOutput.namedConsole(label);
                default:
            }
        }
        throw new NotImplementedException(loggingTarget.toString());
    }

    public MongoDb3Resolver() {
        super(MongoClient.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final TestProperties props = TestPropertySource.createProperties(context);
        final Mongod mongod = Mongod.builder()
                .processOutput(Derive.given(Name.class)
                        .state(ProcessOutput.class)
                        .deriveBy(name -> getProcessOutput(
                                LoggingTarget.getLoggingTarget(LoggingTarget.STATUS_LOGGER), name.value())))
                .processConfig(Start.to(ProcessConfig.class)
                        .initializedWith(ProcessConfig.defaults().withStopTimeoutInMillis(BUILDER_TIMEOUT_MILLIS))
                        .withTransitionLabel("create default"))
                // workaround for https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/309
                .packageOfDistribution(new PackageOfCommandDistribution() {

                    @Override
                    protected Package packageOf(
                            Command command, Distribution distribution, DistributionBaseUrl baseUrl) {
                        if (distribution.platform().operatingSystem().type() == OSType.Windows) {
                            final Package relativePackage = legacyPackageResolverFactory()
                                    .apply(command)
                                    .packageFor(distribution);
                            final FileSet.Builder fileSetBuilder = FileSet.builder()
                                    .addEntry(FileType.Library, "ssleay32.dll")
                                    .addEntry(FileType.Library, "libeay32.dll");
                            relativePackage.fileSet().entries().forEach(fileSetBuilder::addEntries);
                            return Package.builder()
                                    .archiveType(relativePackage.archiveType())
                                    .fileSet(fileSetBuilder.build())
                                    .url(baseUrl.value() + relativePackage.url())
                                    .hint(relativePackage.hint())
                                    .build();
                        }
                        return super.packageOf(command, distribution, baseUrl);
                    }
                })
                .build();
        ExtensionContextAnchor.setAttribute(MongoClientHolder.class, new MongoClientHolder(mongod, props), context);
    }

    @Override
    public MongoClient resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return ExtensionContextAnchor.getAttribute(MongoClientHolder.class, MongoClientHolder.class, extensionContext)
                .get();
    }

    public enum LoggingTarget {
        CONSOLE,
        STATUS_LOGGER;

        public static LoggingTarget getLoggingTarget(final LoggingTarget defaultValue) {
            return LoggingTarget.valueOf(System.getProperty(LOGGING_TARGET_PROPERTY, defaultValue.name()));
        }
    }

    private static final class MongoClientHolder implements CloseableResource, Supplier<MongoClient> {
        private final ReachedState<RunningMongodProcess> state;
        private final MongoClient mongoClient;

        public MongoClientHolder(final Mongod mongod, final TestProperties props) {
            state = mongod.start(Version.Main.V3_6);
            final RunningMongodProcess mongodProcess = state.current();
            final ServerAddress addr = mongodProcess.getServerAddress();
            mongoClient = new MongoClient(addr.getHost(), addr.getPort());
            props.setProperty(MongoDb3TestConstants.PROP_NAME_PORT, addr.getPort());
        }

        @Override
        public MongoClient get() {
            return mongoClient;
        }

        @Override
        public void close() throws Exception {
            mongoClient.close();
            state.close();
        }
    }

    private static final class StatusLoggerStreamProcessor implements StreamProcessor {

        private final Level level;

        public StatusLoggerStreamProcessor(Level level) {
            this.level = level;
        }

        public void process(String line) {
            LOGGER.log(level, () -> stripLineEndings(line));
        }

        public void onProcessed() {}

        protected String stripLineEndings(String line) {
            // we still need to remove line endings that are passed on by
            // StreamToLineProcessor...
            return line.replaceAll("[\n\r]+", "");
        }
    }
}

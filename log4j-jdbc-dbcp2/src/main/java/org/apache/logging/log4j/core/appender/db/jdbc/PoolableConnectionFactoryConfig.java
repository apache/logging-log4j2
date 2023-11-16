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
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Configures an Apache Commons DBCP {@link PoolableConnectionFactory}.
 *
 * @since 2.11.2
 */
@Plugin(name = "PoolableConnectionFactory", category = Core.CATEGORY_NAME, printObject = true)
public class PoolableConnectionFactoryConfig {

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PoolableConnectionFactoryConfig> {

        private static final PoolableConnectionFactory DEFAULT = new PoolableConnectionFactory(null, null);

        /**
         * Internal constant to indicate the level is not set.
         */
        private static final int UNKNOWN_TRANSACTION_ISOLATION = -1;

        // All of these instance variables match DBCP WRT Boolean vs. boolean.
        // All of these defaults are the same as in PoolableConnectionFactory.

        @PluginBuilderAttribute
        private boolean cacheState;

        // TODO
        @PluginElement("ConnectionInitSqls")
        private String[] connectionInitSqls;

        @PluginBuilderAttribute
        private Boolean defaultAutoCommit;

        @PluginBuilderAttribute
        private String defaultCatalog;

        @PluginBuilderAttribute
        private Integer defaultQueryTimeoutSeconds = DEFAULT.getDefaultQueryTimeout();

        @PluginBuilderAttribute
        private Boolean defaultReadOnly;

        @PluginBuilderAttribute
        private int defaultTransactionIsolation = UNKNOWN_TRANSACTION_ISOLATION;

        // TODO
        @PluginElement("DisconnectionSqlCodes")
        private String[] disconnectionSqlCodes = (String[])
                (DEFAULT.getDisconnectionSqlCodes() == null
                        ? null
                        : DEFAULT.getDisconnectionSqlCodes().toArray());

        @PluginBuilderAttribute
        private boolean autoCommitOnReturn = DEFAULT.isAutoCommitOnReturn();

        @PluginBuilderAttribute
        private boolean fastFailValidation = DEFAULT.isFastFailValidation();

        @PluginBuilderAttribute
        private long maxConnLifetimeMillis = -1;

        @PluginBuilderAttribute
        private int maxOpenPreparedStatements = GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL_PER_KEY;

        @PluginBuilderAttribute
        private boolean poolStatements;

        @PluginBuilderAttribute
        private boolean rollbackOnReturn = DEFAULT.isRollbackOnReturn();

        @PluginBuilderAttribute
        private String validationQuery;

        @PluginBuilderAttribute
        private int validationQueryTimeoutSeconds = -1;

        private List<String> asList(final String[] array) {
            return array == null ? null : Arrays.asList(array);
        }

        @Override
        public PoolableConnectionFactoryConfig build() {
            return new PoolableConnectionFactoryConfig(
                    cacheState,
                    asList(connectionInitSqls),
                    defaultAutoCommit,
                    defaultCatalog,
                    defaultQueryTimeoutSeconds,
                    defaultReadOnly,
                    defaultTransactionIsolation,
                    asList(disconnectionSqlCodes),
                    autoCommitOnReturn,
                    fastFailValidation,
                    maxConnLifetimeMillis,
                    maxOpenPreparedStatements,
                    poolStatements,
                    rollbackOnReturn,
                    validationQuery,
                    validationQueryTimeoutSeconds);
        }

        public Builder setAutoCommitOnReturn(final boolean autoCommitOnReturn) {
            this.autoCommitOnReturn = autoCommitOnReturn;
            return this;
        }

        public Builder setCacheState(final boolean cacheState) {
            this.cacheState = cacheState;
            return this;
        }

        public Builder setConnectionInitSqls(final String... connectionInitSqls) {
            this.connectionInitSqls = connectionInitSqls;
            return this;
        }

        public Builder setDefaultAutoCommit(final Boolean defaultAutoCommit) {
            this.defaultAutoCommit = defaultAutoCommit;
            return this;
        }

        public Builder setDefaultCatalog(final String defaultCatalog) {
            this.defaultCatalog = defaultCatalog;
            return this;
        }

        public Builder setDefaultQueryTimeoutSeconds(final Integer defaultQueryTimeoutSeconds) {
            this.defaultQueryTimeoutSeconds = defaultQueryTimeoutSeconds;
            return this;
        }

        public Builder setDefaultReadOnly(final Boolean defaultReadOnly) {
            this.defaultReadOnly = defaultReadOnly;
            return this;
        }

        public Builder setDefaultTransactionIsolation(final int defaultTransactionIsolation) {
            this.defaultTransactionIsolation = defaultTransactionIsolation;
            return this;
        }

        public Builder setDisconnectionSqlCodes(final String... disconnectionSqlCodes) {
            this.disconnectionSqlCodes = disconnectionSqlCodes;
            return this;
        }

        public Builder setFastFailValidation(final boolean fastFailValidation) {
            this.fastFailValidation = fastFailValidation;
            return this;
        }

        public Builder setMaxConnLifetimeMillis(final long maxConnLifetimeMillis) {
            this.maxConnLifetimeMillis = maxConnLifetimeMillis;
            return this;
        }

        public Builder setMaxOpenPreparedStatements(final int maxOpenPreparedStatements) {
            this.maxOpenPreparedStatements = maxOpenPreparedStatements;
            return this;
        }

        public Builder setPoolStatements(final boolean poolStatements) {
            this.poolStatements = poolStatements;
            return this;
        }

        public Builder setRollbackOnReturn(final boolean rollbackOnReturn) {
            this.rollbackOnReturn = rollbackOnReturn;
            return this;
        }

        public Builder setValidationQuery(final String validationQuery) {
            this.validationQuery = validationQuery;
            return this;
        }

        public Builder setValidationQueryTimeoutSeconds(final int validationQueryTimeoutSeconds) {
            this.validationQueryTimeoutSeconds = validationQueryTimeoutSeconds;
            return this;
        }
    }

    // ALL of these instance variables match DBCP WRT Boolean vs. boolean.

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final boolean cacheState;
    private final Collection<String> connectionInitSqls;
    private final Boolean defaultAutoCommit;
    private final String defaultCatalog;
    private final Integer defaultQueryTimeoutSeconds;
    private final Boolean defaultReadOnly;
    private final int defaultTransactionIsolation;
    private final Collection<String> disconnectionSqlCodes;
    private final boolean autoCommitOnReturn;
    private final boolean fastFailValidation;
    private final long maxConnLifetimeMillis;
    private final int maxOpenPreparedStatements;
    private final boolean poolStatements;
    private final boolean rollbackOnReturn;
    private final String validationQuery;

    private final int validationQueryTimeoutSeconds;

    private PoolableConnectionFactoryConfig(
            final boolean cacheState,
            final Collection<String> connectionInitSqls,
            final Boolean defaultAutoCommit,
            final String defaultCatalog,
            final Integer defaultQueryTimeoutSeconds,
            final Boolean defaultReadOnly,
            final int defaultTransactionIsolation,
            final Collection<String> disconnectionSqlCodes,
            final boolean enableAutoCommitOnReturn,
            final boolean fastFailValidation,
            final long maxConnLifetimeMillis,
            final int maxOpenPreparedStatements,
            final boolean poolStatements,
            final boolean rollbackOnReturn,
            final String validationQuery,
            final int validationQueryTimeoutSeconds) {
        this.cacheState = cacheState;
        this.connectionInitSqls = connectionInitSqls;
        this.defaultAutoCommit = defaultAutoCommit;
        this.defaultCatalog = Strings.trimToNull(defaultCatalog);
        this.defaultQueryTimeoutSeconds = defaultQueryTimeoutSeconds;
        this.defaultReadOnly = defaultReadOnly;
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        this.disconnectionSqlCodes = disconnectionSqlCodes;
        this.autoCommitOnReturn = enableAutoCommitOnReturn;
        this.fastFailValidation = fastFailValidation;
        this.maxConnLifetimeMillis = maxConnLifetimeMillis;
        this.maxOpenPreparedStatements = maxOpenPreparedStatements;
        this.poolStatements = poolStatements;
        this.rollbackOnReturn = rollbackOnReturn;
        this.validationQuery = Strings.trimToNull(validationQuery);
        this.validationQueryTimeoutSeconds = validationQueryTimeoutSeconds;
    }

    public void init(final PoolableConnectionFactory poolableConnectionFactory) {
        if (poolableConnectionFactory != null) {
            StatusLogger.getLogger()
                    .debug("Initializing PoolableConnectionFactory {} with {}", poolableConnectionFactory, this);
            poolableConnectionFactory.setCacheState(cacheState);
            poolableConnectionFactory.setConnectionInitSql(connectionInitSqls);
            poolableConnectionFactory.setDefaultAutoCommit(defaultAutoCommit);
            poolableConnectionFactory.setDefaultCatalog(defaultCatalog);
            poolableConnectionFactory.setDefaultQueryTimeout(defaultQueryTimeoutSeconds);
            poolableConnectionFactory.setDefaultReadOnly(defaultReadOnly);
            poolableConnectionFactory.setDefaultTransactionIsolation(defaultTransactionIsolation);
            poolableConnectionFactory.setDisconnectionSqlCodes(disconnectionSqlCodes);
            poolableConnectionFactory.setAutoCommitOnReturn(autoCommitOnReturn);
            poolableConnectionFactory.setFastFailValidation(fastFailValidation);
            poolableConnectionFactory.setMaxConnLifetimeMillis(maxConnLifetimeMillis);
            poolableConnectionFactory.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
            poolableConnectionFactory.setPoolStatements(poolStatements);
            poolableConnectionFactory.setRollbackOnReturn(rollbackOnReturn);
            poolableConnectionFactory.setValidationQuery(validationQuery);
            poolableConnectionFactory.setValidationQueryTimeout(validationQueryTimeoutSeconds);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "PoolableConnectionFactoryConfig [cacheState=%s, connectionInitSqls=%s, defaultAutoCommit=%s, defaultCatalog=%s, defaultQueryTimeoutSeconds=%s, defaultReadOnly=%s, defaultTransactionIsolation=%s, disconnectionSqlCodes=%s, enableAutoCommitOnReturn=%s, fastFailValidation=%s, maxConnLifetimeMillis=%s, maxOpenPreparedStatements=%s, poolStatements=%s, rollbackOnReturn=%s, validationQuery=%s, validationQueryTimeoutSeconds=%s]",
                cacheState,
                connectionInitSqls,
                defaultAutoCommit,
                defaultCatalog,
                defaultQueryTimeoutSeconds,
                defaultReadOnly,
                defaultTransactionIsolation,
                disconnectionSqlCodes,
                autoCommitOnReturn,
                fastFailValidation,
                maxConnLifetimeMillis,
                maxOpenPreparedStatements,
                poolStatements,
                rollbackOnReturn,
                validationQuery,
                validationQueryTimeoutSeconds);
    }
}

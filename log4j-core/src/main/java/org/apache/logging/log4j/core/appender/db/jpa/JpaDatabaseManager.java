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
package org.apache.logging.log4j.core.appender.db.jpa;

import java.lang.reflect.Constructor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;

/**
 * An {@link AbstractDatabaseManager} implementation for relational databases accessed via JPA.
 */
public final class JpaDatabaseManager extends AbstractDatabaseManager {
    private static final JPADatabaseManagerFactory FACTORY = new JPADatabaseManagerFactory();

    private final String entityClassName;
    private final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor;
    private final String persistenceUnitName;

    private EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private EntityTransaction transaction;

    private JpaDatabaseManager(final String name, final int bufferSize,
                               final Class<? extends AbstractLogEventWrapperEntity> entityClass,
                               final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor,
                               final String persistenceUnitName) {
        super(name, bufferSize);
        this.entityClassName = entityClass.getName();
        this.entityConstructor = entityConstructor;
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    protected void startupInternal() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(this.persistenceUnitName);
    }

    @Override
    protected boolean shutdownInternal() {
        boolean closed = true;
        if (this.entityManager != null || this.transaction != null) {
            closed &= this.commitAndClose();
        }
        if (this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
        return closed;
    }

    @Override
    protected void connectAndStart() {
        try {
            this.entityManager = this.entityManagerFactory.createEntityManager();
            this.transaction = this.entityManager.getTransaction();
            this.transaction.begin();
        } catch (final Exception e) {
            throw new AppenderLoggingException(
                    "Cannot write logging event or flush buffer; manager cannot create EntityManager or transaction.", e
            );
        }
    }

    @Override
    protected void writeInternal(final LogEvent event) {
        if (!this.isRunning() || this.entityManagerFactory == null || this.entityManager == null
                || this.transaction == null) {
            throw new AppenderLoggingException(
                    "Cannot write logging event; JPA manager not connected to the database.");
        }

        AbstractLogEventWrapperEntity entity;
        try {
            entity = this.entityConstructor.newInstance(event);
        } catch (final Exception e) {
            throw new AppenderLoggingException("Failed to instantiate entity class [" + this.entityClassName + "].", e);
        }

        try {
            this.entityManager.persist(entity);
        } catch (final Exception e) {
            if (this.transaction != null && this.transaction.isActive()) {
                this.transaction.rollback();
                this.transaction = null;
            }
            throw new AppenderLoggingException("Failed to insert record for log event in JPA manager: " +
                    e.getMessage(), e);
        }
    }

    @Override
    protected boolean commitAndClose() {
        boolean closed = true;
        try {
            if (this.transaction != null && this.transaction.isActive()) {
                this.transaction.commit();
            }
        } catch (final Exception e) {
            if (this.transaction != null && this.transaction.isActive()) {
                this.transaction.rollback();
            }
        } finally {
            this.transaction = null;
            try {
                if (this.entityManager != null && this.entityManager.isOpen()) {
                    this.entityManager.close();
                }
            } catch (final Exception e) {
                logWarn("Failed to close entity manager while logging event or flushing buffer", e);
                closed = false;
            } finally {
                this.entityManager = null;
            }
        }
        return closed;
    }

    /**
     * Creates a JPA manager for use within the {@link JpaAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details, entity class name, etc.
     * @param bufferSize The size of the log event buffer.
     * @param entityClass The fully-qualified class name of the {@link AbstractLogEventWrapperEntity} concrete
     *                    implementation.
     * @param entityConstructor The one-arg {@link LogEvent} constructor for the concrete entity class.
     * @param persistenceUnitName The name of the JPA persistence unit that should be used for persisting log events.
     * @return a new or existing JPA manager as applicable.
     */
    public static JpaDatabaseManager getJPADatabaseManager(final String name, final int bufferSize,
                                                           final Class<? extends AbstractLogEventWrapperEntity>
                                                                   entityClass,
                                                           final Constructor<? extends AbstractLogEventWrapperEntity>
                                                                   entityConstructor,
                                                           final String persistenceUnitName) {

        return AbstractDatabaseManager.getManager(
                name, new FactoryData(bufferSize, entityClass, entityConstructor, persistenceUnitName), FACTORY
        );
    }

    /**
     * Encapsulates data that {@link JPADatabaseManagerFactory} uses to create managers.
     */
    private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
        private final Class<? extends AbstractLogEventWrapperEntity> entityClass;
        private final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor;
        private final String persistenceUnitName;

        protected FactoryData(final int bufferSize, final Class<? extends AbstractLogEventWrapperEntity> entityClass,
                              final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor,
                              final String persistenceUnitName) {
            super(bufferSize);

            this.entityClass = entityClass;
            this.entityConstructor = entityConstructor;
            this.persistenceUnitName = persistenceUnitName;
        }
    }

    /**
     * Creates managers.
     */
    private static final class JPADatabaseManagerFactory implements ManagerFactory<JpaDatabaseManager, FactoryData> {
        @Override
        public JpaDatabaseManager createManager(final String name, final FactoryData data) {
            return new JpaDatabaseManager(
                    name, data.getBufferSize(), data.entityClass, data.entityConstructor, data.persistenceUnitName
            );
        }
    }
}

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
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;

/**
 * An {@link AbstractDatabaseManager} implementation for relational databases accessed via JPA.
 */
public final class JPADatabaseManager extends AbstractDatabaseManager {
    private static final JPADatabaseManagerFactory FACTORY = new JPADatabaseManagerFactory();

    private final String entityClassName;
    private final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor;
    private final String persistenceUnitName;

    private EntityManager entityManager;
    private EntityManagerFactory entityManagerFactory;
    private EntityTransaction transaction;

    private JPADatabaseManager(final String name, final int bufferSize,
                               final Class<? extends AbstractLogEventWrapperEntity> entityClass,
                               final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor,
                               final String persistenceUnitName) {
        super(name, bufferSize);
        this.entityClassName = entityClass.getName();
        this.entityConstructor = entityConstructor;
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    protected void connectInternal() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory(this.persistenceUnitName);
        this.entityManager = this.entityManagerFactory.createEntityManager();
        this.transaction = this.entityManager.getTransaction();
    }

    @Override
    protected void disconnectInternal() {
        this.transaction = null;

        if (this.entityManager != null && this.entityManager.isOpen()) {
            this.entityManager.close();
        }

        if (this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
    }

    @Override
    protected void writeInternal(final LogEvent event) {
        if (!this.isConnected() || this.transaction == null || this.entityManager == null
                || this.entityManagerFactory == null) {
            LOGGER.error("Cannot write logging event; manager [{}] not connected to the database.", this.getName());
            return;
        }

        AbstractLogEventWrapperEntity entity;
        try {
            entity = this.entityConstructor.newInstance(event);
        } catch (final Exception e) {
            LOGGER.error("Failed to instantiate entity class {}.", this.entityClassName, e);
            return;
        }

        try {
            this.transaction.begin();
            this.entityManager.persist(entity);
            this.transaction.commit();
        } catch (final Exception e) {
            LOGGER.error("Failed to persist log event entity.", e);
            this.transaction.rollback();
        }
    }

    /**
     * Creates a JPA manager for use within the {@link JPAAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details, entity class name, etc.
     * @param bufferSize The size of the log event buffer.
     * @param entityClass The fully-qualified class name of the {@link AbstractLogEventWrapperEntity} concrete
     *                    implementation.
     * @param entityConstructor The one-arg {@link LogEvent} constructor for the concrete entity class.
     * @param persistenceUnitName The name of the JPA persistence unit that should be used for persisting log events.
     * @return a new or existing JPA manager as applicable.
     */
    public static JPADatabaseManager getJPADatabaseManager(final String name, final int bufferSize,
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
    private static final class JPADatabaseManagerFactory implements ManagerFactory<JPADatabaseManager, FactoryData> {
        @Override
        public JPADatabaseManager createManager(final String name, final FactoryData data) {
            return new JPADatabaseManager(
                    name, data.getBufferSize(), data.entityClass, data.entityConstructor, data.persistenceUnitName
            );
        }
    }
}

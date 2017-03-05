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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * This Appender writes logging events to a relational database using the Java Persistence API. It requires a
 * pre-configured JPA persistence unit and a concrete implementation of the abstract
 * {@link AbstractLogEventWrapperEntity} class decorated with JPA annotations.
 *
 * @see AbstractLogEventWrapperEntity
 */
@Plugin(name = "JPA", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class JpaAppender extends AbstractDatabaseAppender<JpaDatabaseManager> {

    private final String description;

    private JpaAppender(final String name, final Filter filter, final boolean ignoreExceptions,
            final JpaDatabaseManager manager) {
        super(name, filter, ignoreExceptions, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a JPA appender within the plugin manager.
     *
     * @param name The name of the appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param filter The filter, if any, to use.
     * @param bufferSize If an integer greater than 0, this causes the appender to buffer log events and flush whenever
     *                   the buffer reaches this size.
     * @param entityClassName The fully qualified name of the concrete {@link AbstractLogEventWrapperEntity}
     *                        implementation that has JPA annotations mapping it to a database table.
     * @param persistenceUnitName The name of the JPA persistence unit that should be used for persisting log events.
     * @return a new JPA appender.
     */
    @PluginFactory
    public static JpaAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("bufferSize") final String bufferSize,
            @PluginAttribute("entityClassName") final String entityClassName,
            @PluginAttribute("persistenceUnitName") final String persistenceUnitName) {
        if (Strings.isEmpty(entityClassName) || Strings.isEmpty(persistenceUnitName)) {
            LOGGER.error("Attributes entityClassName and persistenceUnitName are required for JPA Appender.");
            return null;
        }

        final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        try {
            final Class<? extends AbstractLogEventWrapperEntity> entityClass =
                LoaderUtil.loadClass(entityClassName).asSubclass(AbstractLogEventWrapperEntity.class);

            try {
                entityClass.getConstructor();
            } catch (final NoSuchMethodException e) {
                LOGGER.error("Entity class [{}] does not have a no-arg constructor. The JPA provider will reject it.",
                        entityClassName);
                return null;
            }

            final Constructor<? extends AbstractLogEventWrapperEntity> entityConstructor =
                    entityClass.getConstructor(LogEvent.class);

            final String managerName = "jpaManager{ description=" + name + ", bufferSize=" + bufferSizeInt
                    + ", persistenceUnitName=" + persistenceUnitName + ", entityClass=" + entityClass.getName() + '}';

            final JpaDatabaseManager manager = JpaDatabaseManager.getJPADatabaseManager(
                    managerName, bufferSizeInt, entityClass, entityConstructor, persistenceUnitName
            );
            if (manager == null) {
                return null;
            }

            return new JpaAppender(name, filter, ignoreExceptions, manager);
        } catch (final ClassNotFoundException e) {
            LOGGER.error("Could not load entity class [{}].", entityClassName, e);
            return null;
        } catch (final NoSuchMethodException e) {
            LOGGER.error("Entity class [{}] does not have a constructor with a single argument of type LogEvent.",
                    entityClassName);
            return null;
        } catch (final ClassCastException e) {
            LOGGER.error("Entity class [{}] does not extend AbstractLogEventWrapperEntity.", entityClassName);
            return null;
        }
    }
}

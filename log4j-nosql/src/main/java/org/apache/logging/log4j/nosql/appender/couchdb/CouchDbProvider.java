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
package org.apache.logging.log4j.nosql.appender.couchdb;

import java.lang.reflect.Method;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.nosql.appender.NoSqlProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

/**
 * The Apache CouchDB implementation of {@link NoSqlProvider}.
 */
@Plugin(name = "CouchDB", category = "Core", printObject = true)
public final class CouchDbProvider implements NoSqlProvider<CouchDbConnection> {
    private static final int HTTP = 80;
    private static final int HTTPS = 443;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final CouchDbClient client;
    private final String description;

    private CouchDbProvider(final CouchDbClient client, final String description) {
        this.client = client;
        this.description = "couchDb{ " + description + " }";
    }

    @Override
    public CouchDbConnection getConnection() {
        return new CouchDbConnection(this.client);
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating an Apache CouchDB provider within the plugin manager.
     *
     * @param databaseName The name of the database to which log event documents will be written.
     * @param protocol Either "http" or "https," defaults to "http" and mutually exclusive with
     *                 {@code factoryClassName&factoryMethodName!=null}.
     * @param server The host name of the CouchDB server, defaults to localhost and mutually exclusive with
     *               {@code factoryClassName&factoryMethodName!=null}.
     * @param port The port that CouchDB is listening on, defaults to 80 if {@code protocol} is "http" and 443 if
     *             {@code protocol} is "https," and mutually exclusive with
     *             {@code factoryClassName&factoryMethodName!=null}.
     * @param username The username to authenticate against the MongoDB server with, mutually exclusive with
     *                 {@code factoryClassName&factoryMethodName!=null}.
     * @param password The password to authenticate against the MongoDB server with, mutually exclusive with
     *                 {@code factoryClassName&factoryMethodName!=null}.
     * @param factoryClassName A fully qualified class name containing a static factory method capable of returning a
     *                         {@link CouchDbClient} or {@link CouchDbProperties}.
     * @param factoryMethodName The name of the public static factory method belonging to the aforementioned factory
     *                          class.
     * @return a new Apache CouchDB provider.
     */
    @PluginFactory
    public static CouchDbProvider createNoSqlProvider(
            @PluginAttribute("databaseName") final String databaseName,
            @PluginAttribute("protocol") String protocol,
            @PluginAttribute(value = "server", defaultString = "localhost") @ValidHost String server,
            @PluginAttribute(value = "port", defaultString = "0") @ValidPort final String port,
            @PluginAttribute("username") final String username,
            @PluginAttribute(value = "password", sensitive = true) final String password,
            @PluginAttribute("factoryClassName") final String factoryClassName,
            @PluginAttribute("factoryMethodName") final String factoryMethodName) {
        CouchDbClient client;
        String description;
        if (Strings.isNotEmpty(factoryClassName) && Strings.isNotEmpty(factoryMethodName)) {
            try {
                final Class<?> factoryClass = LoaderUtil.loadClass(factoryClassName);
                final Method method = factoryClass.getMethod(factoryMethodName);
                final Object object = method.invoke(null);

                if (object instanceof CouchDbClient) {
                    client = (CouchDbClient) object;
                    description = "uri=" + client.getDBUri();
                } else if (object instanceof CouchDbProperties) {
                    final CouchDbProperties properties = (CouchDbProperties) object;
                    client = new CouchDbClient(properties);
                    description = "uri=" + client.getDBUri() + ", username=" + properties.getUsername()
                            + ", passwordHash=" + NameUtil.md5(password + CouchDbProvider.class.getName())
                            + ", maxConnections=" + properties.getMaxConnections() + ", connectionTimeout="
                            + properties.getConnectionTimeout() + ", socketTimeout=" + properties.getSocketTimeout();
                } else if (object == null) {
                    LOGGER.error("The factory method [{}.{}()] returned null.", factoryClassName, factoryMethodName);
                    return null;
                } else {
                    LOGGER.error("The factory method [{}.{}()] returned an unsupported type [{}].", factoryClassName,
                            factoryMethodName, object.getClass().getName());
                    return null;
                }
            } catch (final ClassNotFoundException e) {
                LOGGER.error("The factory class [{}] could not be loaded.", factoryClassName, e);
                return null;
            } catch (final NoSuchMethodException e) {
                LOGGER.error("The factory class [{}] does not have a no-arg method named [{}].", factoryClassName,
                        factoryMethodName, e);
                return null;
            } catch (final Exception e) {
                LOGGER.error("The factory method [{}.{}()] could not be invoked.", factoryClassName, factoryMethodName,
                        e);
                return null;
            }
        } else if (Strings.isNotEmpty(databaseName)) {
            if (protocol != null && protocol.length() > 0) {
                protocol = protocol.toLowerCase();
                if (!protocol.equals("http") && !protocol.equals("https")) {
                    LOGGER.error("Only protocols [http] and [https] are supported, [{}] specified.", protocol);
                    return null;
                }
            } else {
                protocol = "http";
                LOGGER.warn("No protocol specified, using default port [http].");
            }

            final int portInt = TypeConverters.convert(port, int.class, protocol.equals("https") ? HTTPS : HTTP);

            if (Strings.isEmpty(username) || Strings.isEmpty(password)) {
                LOGGER.error("You must provide a username and password for the CouchDB provider.");
                return null;
            }

            client = new CouchDbClient(databaseName, false, protocol, server, portInt, username, password);
            description = "uri=" + client.getDBUri() + ", username=" + username + ", passwordHash="
                    + NameUtil.md5(password + CouchDbProvider.class.getName());
        } else {
            LOGGER.error("No factory method was provided so the database name is required.");
            return null;
        }

        return new CouchDbProvider(client, description);
    }
}

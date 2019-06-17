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
package org.apache.logging.log4j.spring.cloud.config.client;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 *
 */
public class Log4j2CloudConfigLoggingSystem extends Log4J2LoggingSystem {
    private static final String FILE_PROTOCOL = "file";
    private static final String HTTPS = "https";
    private Logger LOGGER = StatusLogger.getLogger();

    public Log4j2CloudConfigLoggingSystem(ClassLoader loader) {
        super(loader);
    }

    @Override
    protected String[] getStandardConfigLocations() {
        String[] locations = super.getStandardConfigLocations();
        PropertiesUtil props = new PropertiesUtil(new Properties());
        String location = props.getStringProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (location != null) {
            List<String> list = Arrays.asList(super.getStandardConfigLocations());
            list.add(location);
            locations = list.toArray(new String[list.size()]);
        }
        return locations;
    }

    @Override
    protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
        if (logFile != null) {
            this.loadConfiguration(this.getBootPackagedConfigFile("log4j2-file.xml"), logFile);
        } else {
            this.loadConfiguration(this.getBootPackagedConfigFile("log4j2.xml"), logFile);
        }
    }

    private String getBootPackagedConfigFile(String fileName) {
        String defaultPath = ClassUtils.getPackageName(Log4J2LoggingSystem.class);
        defaultPath = defaultPath.replace('.', '/');
        defaultPath = defaultPath + "/" + fileName;
        defaultPath = "classpath:" + defaultPath;
        return defaultPath;
    }

    @Override
    protected void loadConfiguration(String location, LogFile logFile) {
        Assert.notNull(location, "Location must not be null");
        try {
            LoggerContext ctx = getLoggerContext();
            URL url = ResourceUtils.getURL(location);
            ConfigurationSource source = getConfigurationSource(url);
            ctx.start(ConfigurationFactory.getInstance().getConfiguration(ctx, source));
        }
        catch (Exception ex) {
            throw new IllegalStateException(
                "Could not initialize Log4J2 logging from " + location, ex);
        }
    }

    private ConfigurationSource getConfigurationSource(URL url) throws IOException, URISyntaxException {
        URLConnection urlConnection = url.openConnection();
        AuthorizationProvider provider = ConfigurationFactory.getAuthorizationProvider();
        if (provider != null) {
            provider.addAuthorization(urlConnection);
        }
        if (url.getProtocol().equals(HTTPS)) {
            SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration();
            if (sslConfiguration != null) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
                if (!sslConfiguration.isVerifyHostName()) {
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
                }
            }
        }
        File file = FileUtils.fileFromUri(url.toURI());
        if (file != null) {
            return new ConfigurationSource(urlConnection.getInputStream(), FileUtils.fileFromUri(url.toURI()));
        } else {
            return new ConfigurationSource(urlConnection.getInputStream(), url, urlConnection.getLastModified());
        }
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }
}

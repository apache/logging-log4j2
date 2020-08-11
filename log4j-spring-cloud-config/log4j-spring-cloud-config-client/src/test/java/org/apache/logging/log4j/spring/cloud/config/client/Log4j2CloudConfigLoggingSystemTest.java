package org.apache.logging.log4j.spring.cloud.config.client;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Log4j2CloudConfigLoggingSystemTest {

    @Test
    public void getStandardConfigLocations() {
        String customLog4j2Location = "classpath:my_custom_log4j2.properties";
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, customLog4j2Location);
        Log4j2CloudConfigLoggingSystem cloudLoggingSystem = new Log4j2CloudConfigLoggingSystem(this.getClass().getClassLoader());
        List<String> standardConfigLocations = Arrays.asList(cloudLoggingSystem.getStandardConfigLocations());
        assertTrue(standardConfigLocations.contains(customLog4j2Location));

    }
}
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rgoers on 1/1/17.
 */
public class CronRolloverApp {


    private static Logger logger;

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "target/test-classes/log4j-cronRolloverApp.xml");
        logger = LogManager.getLogger(CronRolloverApp.class);
        try {
            for (int i = 1; i <= 240; i++) {
                logger.info("Hello");
                Thread.sleep(1 * 1000);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error("Excepcion general", e);
        }
    }
}

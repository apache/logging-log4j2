package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

public class LoggerContextChangeTest {
    private static String targetFileName = "log4j2-3182.xml";
    private static String targetErrorFileName = "log4j2-3182-error.xml";
    private static String tmpFileName = "log4j2-3182-tmp.xml";
    private static String destDir = "target/test-classes/";

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.configurationFile", "classpath:" + targetFileName);
    }


    @Test
    public void onChangeTest() {
        String errorFileName = destDir + targetErrorFileName;
        String originFileName = destDir + targetFileName;
        String tmpFile = destDir + tmpFileName;
        wait(10);
        updateConfigFileModTime(originFileName, errorFileName, tmpFile);
        wait(30);
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        Integer kafkaThreads = 0;
        Integer consoleThreads = 0;
        for (Thread thread:allThreads) {
            if (thread.getName().contains("AsyncKafka")) {
                kafkaThreads++;
            }
            if (thread.getName().contains("AsyncCONSOLE")) {
                consoleThreads++;
            }
        }
        Assert.assertTrue(kafkaThreads <= 1);
        Assert.assertTrue(consoleThreads <= 1);
        updateConfigFileModTime(originFileName, tmpFile, errorFileName);
    }

    private void wait(int seconds) {
        try {
            Logger logger = LogManager.getLogger();
            logger.info("Test");
            Thread.sleep(seconds * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将原始文件改为目的文件
     */
    private void updateConfigFileModTime(String originFileName, String destFileName, String tmpFileName) {
        File originFile = new File(originFileName);
        if (originFile.exists()) {
            originFile.renameTo(new File(tmpFileName));
        }
        File destFile = new File(destFileName);
        if (destFile.exists()) {
            destFile.renameTo(new File(originFileName));
        }
    }
}

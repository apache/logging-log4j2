package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

@LoggerContextSource("log4j2-3182.xml")
public class LoggerContextChangeTest {
    private String targetFileName = "log4j2-3182.xml";
    private String targetErrorFileName = "log4j2-3182-error.xml";
    private String tmpFileName = "log4j2-3182-tmp.xml";

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.configurationFile", "classpath:log4j2-3182.xml");
    }


    @Test
    public void onChangeTest() {
        String errorFileName = "target/test-classes/" + targetErrorFileName;
        String originFileName = "target/test-classes/" + targetFileName;
        String tmpFile = "target/test-classes/" + tmpFileName;
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

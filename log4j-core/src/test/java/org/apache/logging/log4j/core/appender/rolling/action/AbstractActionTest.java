package org.apache.logging.log4j.core.appender.rolling.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.StatusLoggerLevel;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;

@StatusLoggerLevel("WARN")
public class AbstractActionTest {

    // Test for LOG4J2-2658
    @Test
    public void testExceptionsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new TestAction().run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList).hasSize(1);
        StatusData statusData = statusDataList.get(0);
        assertThat(statusData.getLevel()).isEqualTo(Level.WARN);
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage).contains("Exception reported by action 'class org.apache."
                + "logging.log4j.core.appender.rolling.action.AbstractActionTest$TestAction' java.io.IOException: "
                + "failed" + System.lineSeparator()
                + "\tat org.apache.logging.log4j.core.appender.rolling.action.AbstractActionTest"
                + "$TestAction.execute(AbstractActionTest.java:");
    }

    @Test
    public void testRuntimeExceptionsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new IllegalStateException();
            }
        }.run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList).hasSize(1);
        StatusData statusData = statusDataList.get(0);
        assertThat(statusData.getLevel()).isEqualTo(Level.WARN);
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage).contains("Exception reported by action");
    }

    @Test
    public void testErrorsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new AssertionError();
            }
        }.run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList).hasSize(1);
        StatusData statusData = statusDataList.get(0);
        assertThat(statusData.getLevel()).isEqualTo(Level.WARN);
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage).contains("Exception reported by action");
    }

    private static final class TestAction extends AbstractAction {
        @Override
        public boolean execute() throws IOException {
            throw new IOException("failed");
        }
    }
}

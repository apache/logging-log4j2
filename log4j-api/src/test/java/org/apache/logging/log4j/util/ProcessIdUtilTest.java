package org.apache.logging.log4j.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessIdUtilTest {

    @Test
    public void processIdTest() throws Exception {
        String processId = ProcessIdUtil.getProcessId();
        assertFalse("ProcessId is default", processId.equals(ProcessIdUtil.DEFAULT_PROCESSID));
    }
}

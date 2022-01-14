package org.apache.logging.log4j.tojul;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Extension of {@link java.util.logging.LogRecord} with lazy get source related methods.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
/* package-local */ final class LazyLogRecord extends LogRecord {

    private static final long serialVersionUID = 6798134264543826471L;

    // parent class LogRecord already has a needToInferCaller but it's private
    private transient boolean stillNeedToInferCaller = true;

    private final String fqcn;

    LazyLogRecord(String fqcn, Level level, String msg) {
        super(level, msg);
        this.fqcn = fqcn;
    }

    @Override
    public String getSourceClassName() {
        if (stillNeedToInferCaller) {
            inferCaller();
        }
        return super.getSourceClassName();
    }

    @Override
    public String getSourceMethodName() {
        if (stillNeedToInferCaller) {
            inferCaller();
        }
        return super.getSourceMethodName();
    }

    private void inferCaller() {
        StackTraceElement location = StackLocatorUtil.calcLocation(fqcn);
        setSourceClassName(location.getClassName());
        setSourceMethodName(location.getMethodName());
        stillNeedToInferCaller = false;
    }
}
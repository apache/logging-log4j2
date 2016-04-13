package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * Mutable implementation of the {@code LogEvent} interface.
 */
public class MutableLogEvent implements LogEvent {
    private String loggerFqcn;
    private Marker marker;
    private Level level;
    private String loggerName;
    private Message message;
    private Throwable thrown;
    private long timeMillis;
    private Map<String, String> contextMap;
    private ThreadContext.ContextStack contextStack;
    private long threadId;
    private String threadName;
    private int threadPriority;
    private boolean includeLocation;
    private boolean endOfBatch = false;
    private long nanoTime;
    // private ThrowableProxy thrownProxy;
    // private StackTraceElement source;

    @Override
    public String getLoggerFqcn() {
        return loggerFqcn;
    }

    public void setLoggerFqcn(String loggerFqcn) {
        this.loggerFqcn = loggerFqcn;
    }

    @Override
    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    @Override
    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        if (thrown != null) {
            return new ThrowableProxy(thrown);
        }
        return null;
    }

    @Override
    public Map<String, String> getContextMap() {
        return contextMap;
    }

    public void setContextMap(Map<String, String> contextMap) {
        this.contextMap = contextMap;
    }

    @Override
    public ThreadContext.ContextStack getContextStack() {
        return contextStack;
    }

    public void setContextStack(ThreadContext.ContextStack contextStack) {
        this.contextStack = contextStack;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    @Override
    public StackTraceElement getSource() {
        if (loggerFqcn == null || !includeLocation) {
            return null;
        }
        return Log4jLogEvent.calcLocation(loggerFqcn);
    }

    @Override
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    @Override
    public void setIncludeLocation(boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    @Override
    public boolean isEndOfBatch() {
        return endOfBatch;
    }

    @Override
    public void setEndOfBatch(boolean endOfBatch) {
        this.endOfBatch = endOfBatch;
    }

    @Override
    public long getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Creates a LogEventProxy that can be serialized.
     * @return a LogEventProxy.
     */
    protected Object writeReplace() {
        getThrownProxy(); // ensure ThrowableProxy is initialized
        return new Log4jLogEvent.LogEventProxy(this, this.includeLocation);
    }

    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}

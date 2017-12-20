package org.apache.logging.log4j.message;

import java.util.Objects;

/**
 * An alternative to {@link java.lang.StackTraceElement} for describing source
 * code locations. It differs in that all fields are nullable.
 */
public final class SourceLocation implements java.io.Serializable {
    private static final long serialVersionUID = 4324770886316212646L;

    public static SourceLocation valueOf(StackTraceElement source) {
        return new SourceLocation(source.getClassName(), source.getMethodName(), source.isNativeMethod(), source.getFileName(), source.getLineNumber());
    }

    final private String className;
    final private boolean isNativeMethod;
    final private String methodName;
    final private String fileName;
    final private Integer lineNumber;

    private String toStringMemo;

    public SourceLocation(String className, String methodName, String fileName, Integer lineNumber) {
        this(className, methodName, false, fileName, lineNumber);
    }

    SourceLocation(String className, String methodName, boolean isNativeMethod, String fileName, Integer lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.isNativeMethod = isNativeMethod;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        //Should mirror StackTraceElement#toString, for backwards compatibility.
        if (toStringMemo == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(className);
            builder.append('.');
            builder.append(methodName);
            builder.append('(');
            if (isNativeMethod) {
                builder.append("Native Method");
            } else {
                if (fileName == null) {
                    builder.append("Unknown Source");
                } else {
                    builder.append(fileName);
                    if (lineNumber != null) {
                        builder.append(':');
                        builder.append(lineNumber);
                    }
                }
            }
            builder.append(')');

            toStringMemo = builder.toString();
        }
        return toStringMemo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceLocation that = (SourceLocation) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(lineNumber, that.lineNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, fileName, lineNumber);
    }
}

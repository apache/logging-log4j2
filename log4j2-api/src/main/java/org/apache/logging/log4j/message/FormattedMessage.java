package org.apache.logging.log4j.message;

/**
 *
 */
public interface FormattedMessage extends Message
{
    void setFormat(String format);

    String getFormat();
}

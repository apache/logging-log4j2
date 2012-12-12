package org.apache.logging.log4j.message;

/**
 * Provides an abstract superclass for MessageFactory implementations with default implementations.
 * <p>
 * This class is immutable.
 * </p>
 * 
 * @version $Id$
 */
public abstract class AbstractMessageFactory implements MessageFactory {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.Object)
     */
    public Message newMessage(final Object message) {
        return new ObjectMessage(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String)
     */
    public Message newMessage(final String message) {
        return new SimpleMessage(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(java.lang.String, java.lang.Object)
     */
    public abstract Message newMessage(String message, Object... params);
}

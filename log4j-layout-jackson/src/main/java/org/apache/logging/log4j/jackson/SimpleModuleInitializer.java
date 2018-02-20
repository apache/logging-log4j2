package org.apache.logging.log4j.jackson;

import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Used to set up {@link SimpleModule} from different {@link SimpleModule} subclasses.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class SimpleModuleInitializer {
    public void initialize(final SimpleModule simpleModule, final boolean objectMessageAsJsonObject) {
        // Workaround because mix-ins do not work for classes that already have a built-in deserializer.
        // See Jackson issue 429.
        simpleModule.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        simpleModule.addDeserializer(ContextStack.class, new MutableThreadContextStackDeserializer());
        if (objectMessageAsJsonObject) {
            simpleModule.addSerializer(ObjectMessage.class, new ObjectMessageSerializer());
        }
        simpleModule.addSerializer(Message.class, new MessageSerializer());
    }
}
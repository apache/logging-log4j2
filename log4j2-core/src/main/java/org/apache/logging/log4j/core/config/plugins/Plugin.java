package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

    public String name();
    public String type();
    public String elementType() default NULL;
    public boolean printObject() default false;
    public boolean deferChildren() default false;

    public static final String NULL = "";
}

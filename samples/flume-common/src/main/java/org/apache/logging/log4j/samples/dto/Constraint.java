package org.apache.logging.log4j.samples.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraint {

	boolean required() default false;

	String pattern() default "";

	int minLength() default -1;

	int maxLength() default -1;

	int totalDigits() default -1;
}

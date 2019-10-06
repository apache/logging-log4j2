package org.apache.logging.log4j.plugins.inject;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
// TODO: annotation processor to validate type matches (help avoid runtime errors)
public @interface InjectorStrategy {
    Class<? extends ConfigurationInjector<? extends Annotation, ?>> value();
}

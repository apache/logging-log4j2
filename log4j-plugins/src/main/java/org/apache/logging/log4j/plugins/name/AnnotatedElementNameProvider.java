package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.util.ReflectionUtil;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Extracts a specified name for some configurable annotated element. A specified name is one given in a non-empty
 * string in an annotation as opposed to relying on the default name taken from the annotated element itself.
 *
 * @param <A> plugin configuration annotation
 */
public interface AnnotatedElementNameProvider<A extends Annotation> {

    static String getName(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Optional<String> specifiedName = getSpecifiedNameForAnnotation(annotation);
            if (specifiedName.isPresent()) {
                return specifiedName.get();
            }
        }

        if (element instanceof Field) {
            return ((Field) element).getName();
        }

        if (element instanceof Method) {
            final Method method = (Method) element;
            final String methodName = method.getName();
            if (methodName.startsWith("set")) {
                return Introspector.decapitalize(methodName.substring(3));
            }
            if (methodName.startsWith("with")) {
                return Introspector.decapitalize(methodName.substring(4));
            }
            return method.getParameters()[0].getName();
        }

        if (element instanceof Parameter) {
            return ((Parameter) element).getName();
        }

        throw new IllegalArgumentException("Unknown element type for naming: " + element.getClass());
    }

    static <A extends Annotation> Optional<String> getSpecifiedNameForAnnotation(final A annotation) {
        return Optional.ofNullable(annotation.annotationType().getAnnotation(NameProvider.class))
                .map(NameProvider::value)
                .flatMap(clazz -> {
                    @SuppressWarnings("unchecked") final AnnotatedElementNameProvider<A> factory =
                            (AnnotatedElementNameProvider<A>) ReflectionUtil.instantiate(clazz);
                    return factory.getSpecifiedName(annotation);
                });
    }

    /**
     * Returns the specified name from this annotation if given or {@code Optional.empty()} if none given.
     *
     * @param annotation annotation value of configuration element
     * @return specified name of configuration element or empty if none specified
     */
    Optional<String> getSpecifiedName(final A annotation);
}

package org.apache.logging.log4j.plugins.bind;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverterRegistry;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractOptionBinder<E extends AnnotatedElement> implements OptionBinder {
    protected static final Logger LOGGER = StatusLogger.getLogger();

    final E element;
    final String name;
    private final Type injectionType;
    private final Collection<ConstraintValidator<?>> validators;

    AbstractOptionBinder(final E element, final Function<E, Type> injectionTypeExtractor) {
        this.element = Objects.requireNonNull(element);
        this.name = AnnotatedElementNameProvider.getName(element);
        Objects.requireNonNull(injectionTypeExtractor);
        this.injectionType = Objects.requireNonNull(injectionTypeExtractor.apply(element));
        this.validators = ConstraintValidators.findValidators(element.getAnnotations());
    }

    @Override
    public Object bindString(final Object target, final String value) {
        Object convertedValue = null;
        if (value != null) {
            final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(injectionType);
            try {
                convertedValue = converter.convert(value);
            } catch (final Exception e) {
                LOGGER.error("Cannot convert string '{}' to type {} in option named {}. {}", value, injectionType, name, e);
            }
        }
        return bindObject(target, convertedValue);
    }

    void validate(final Object value) {
        boolean valid = true;
        for (ConstraintValidator<?> validator : validators) {
            valid &= validator.isValid(name, value);
        }
        // FIXME: this doesn't seem to work properly with primitive types
//        if (valid && value != null && !TypeUtil.isAssignable(injectionType, value.getClass())) {
//            LOGGER.error("Cannot bind value of type {} to option {} with type {}", value.getClass(), name, injectionType);
//            valid = false;
//        }
        if (!valid) {
            throw new OptionBindingException(name, value);
        }
    }

}

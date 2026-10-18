package com.clipper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated element could be null under some circumstances.
 * <p>
 * In general, this means developers will have to read the documentation to
 * determine when a null value is acceptable and whether it is necessary to
 * check for a null value.
 * <p>
 * This annotation is useful mostly for overriding a {@link } annotation.
 * Static analysis tools should generally treat the annotated items as though
 * they had no annotation, unless they are configured to minimize false
 * negatives. Use {@link } to indicate that the element value should
 * always be checked for a null value.
 * <p>
 * When this annotation is applied to a method it applies to the method return
 * value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
public @interface Nullable {

}
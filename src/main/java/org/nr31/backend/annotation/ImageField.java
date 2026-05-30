package org.nr31.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark fields that refer to an image (e.g. by ID).
 * Used by the WidgetSchemaService to add x-image metadata to the generated JSON schema.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageField {
}

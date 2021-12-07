package com.anderb.breskulorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.anderb.breskulorm.annotation.GenerationType.IDENTITY;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String value() default "";

    GenerationType generatedValue() default IDENTITY;
}

package com.library.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** BCrypt limits input by UTF-8 bytes, whereas @Size counts Java characters. */
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = Utf8LengthValidator.class)
public @interface Utf8Length {
    String message() default "密码的UTF-8长度不能超过{max}字节";
    int max();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

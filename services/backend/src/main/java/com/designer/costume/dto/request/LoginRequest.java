package com.designer.costume.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 72) @BcryptPasswordLimit String password) {

    @Documented
    @Constraint(validatedBy = BcryptPasswordLimit.Validator.class)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface BcryptPasswordLimit {

        String message() default "must not exceed 72 UTF-8 bytes";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        final class Validator implements ConstraintValidator<BcryptPasswordLimit, String> {

            @Override
            public boolean isValid(String value, ConstraintValidatorContext context) {
                return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
            }
        }
    }
}

package com.designer.costume.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordPolicy.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {

    String message() default "Password does not meet the required strength";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    final class Validator implements ConstraintValidator<PasswordPolicy, String> {

        @Override
        public boolean isValid(String password, ConstraintValidatorContext context) {
            if (password == null) {
                return true;
            }
            int byteLength = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            return password.length() >= 8
                    && byteLength <= 72
                    && password.chars().anyMatch(Character::isUpperCase)
                    && password.chars().anyMatch(Character::isLowerCase)
                    && password.chars().anyMatch(Character::isDigit)
                    && password.chars().anyMatch(character ->
                            !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));
        }
    }
}

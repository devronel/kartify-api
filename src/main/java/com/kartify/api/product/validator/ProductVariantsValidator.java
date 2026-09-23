package com.kartify.api.product.validator;

import com.kartify.api.contract.ProductVariantPayload;
import com.kartify.api.product.validator.annotation.ValidProductVariant;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductVariantsValidator implements ConstraintValidator<ValidProductVariant, ProductVariantPayload> {

    @Override
    public boolean isValid(ProductVariantPayload request, ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        // Only require variants when hasVariants is explicitly true
        if (!Boolean.TRUE.equals(request.hasVariant())) {
            return true;
        }

        return request.variants() != null && !request.variants().isEmpty();

    }

}

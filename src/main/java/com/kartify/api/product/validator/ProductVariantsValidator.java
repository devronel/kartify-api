package com.kartify.api.product.validator;

import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.validator.annotation.ValidProductVariant;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductVariantsValidator implements ConstraintValidator<ValidProductVariant, ProductCreateRequest> {

    @Override
    public boolean isValid(ProductCreateRequest request, ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        // Only require variants when hasVariants is explicitly true
        if (!Boolean.TRUE.equals(request.hasVariants())) {
            return true;
        }

        return request.variants() != null && !request.variants().isEmpty();

    }

}

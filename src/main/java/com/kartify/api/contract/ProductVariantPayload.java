package com.kartify.api.contract;

import java.util.List;

public interface ProductVariantPayload {
    Boolean hasVariant();
    List<?> variants();
}

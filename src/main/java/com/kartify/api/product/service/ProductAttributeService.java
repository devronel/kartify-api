package com.kartify.api.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.dto.ProductAttributeRequest;
import com.kartify.api.product.dto.ProductAttributeResponse;
import com.kartify.api.product.dto.ProductAttributeValueRequest;
import com.kartify.api.product.dto.ProductAttributeValueResponse;
import com.kartify.api.product.dto.ProductAttributeWithValueResponse;
import com.kartify.api.product.entity.ProductAttribute;
import com.kartify.api.product.entity.ProductAttributeValue;
import com.kartify.api.product.repository.ProductAttributeRepository;
import com.kartify.api.product.repository.ProductAttributeValueRepository;

@Service
public class ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    public ProductAttributeService(
        ProductAttributeRepository productAttributeRepository,
        ProductAttributeValueRepository productAttributeValueRepository
    ){
        this.productAttributeRepository = productAttributeRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
    }

    // --- Create product variant attribute ---
    @Transactional
    public ProductAttributeResponse create(ProductAttributeRequest payload){

        ProductAttribute productAttribute = new ProductAttribute();
        productAttribute.setName(payload.name());

        ProductAttribute createdProductAttribute = productAttributeRepository.save(productAttribute);

        return new ProductAttributeResponse(
            createdProductAttribute.getId(),
            createdProductAttribute.getName()
        );
    }

    // --- Create product variant attribute value ---
    @Transactional
    public ProductAttributeValueResponse createAttributeValue(ProductAttributeValueRequest payload){

        ProductAttribute productAttribute = productAttributeRepository.findById(payload.productAttributeId())
                .orElseThrow(() -> new ResourceNotFoundException("No product attribute found."));

        ProductAttributeValue attributeValue = new ProductAttributeValue();
        attributeValue.setProductAttribute(productAttribute);
        attributeValue.setValue(payload.productAttributeValueName());

        ProductAttributeValue savedAttributeValue = productAttributeValueRepository.save(attributeValue);

        return new ProductAttributeValueResponse(
            savedAttributeValue.getId(), 
            productAttribute.getId(), 
            savedAttributeValue.getValue()
        );

    }

    // --- Get all the arribute with values
    public List<ProductAttributeWithValueResponse> findAllAttributesWithValues(){

        List<ProductAttribute> productAttributes = productAttributeRepository.findAll();

        return productAttributes.stream()
            .map(attribute -> new ProductAttributeWithValueResponse(
                    attribute.getId(),
                    attribute.getName(),
                    attribute.getAttributeValues().stream()
                            .map(value -> new ProductAttributeValueResponse(
                                    value.getId(),
                                    value.getProductAttribute().getId(),
                                    value.getValue()
                            ))
                            .toList()
            ))
            .toList();
    }

}

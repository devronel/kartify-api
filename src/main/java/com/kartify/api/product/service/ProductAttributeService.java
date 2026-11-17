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

    /**
     * Create new variant attribute
     * 
     * @param payload contains the data of new variant attribute
     * @return product attribute response
     */
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


    /**
     * Create new attribute value
     * 
     * Find the variant attribute for this new attribute value
     * to make a relation
     * 
     * 
     * @param payload contains data for attribute value
     * @return product attribute value response
     * @throws ResourceNotFoundException if the parent attribute for this value not found
     */
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

    /**
     * Retrieve all the attribute
     * 
     * Includes each variants attribute its attribute values
     * 
     * @return product attribute with attribute values
    */
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
                    )).toList()
            ))
            .toList();
    }

}

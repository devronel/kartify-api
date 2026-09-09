package com.kartify.api.product.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.category.entity.Category;
import com.kartify.api.category.repository.CategoryRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.dto.ProductResponse;
import com.kartify.api.product.dto.ProductVariantRequest;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductAttributeValue;
import com.kartify.api.product.entity.ProductFile;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductAttributeValueRepository;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.shared.dto.UploadedFileResponse;
import com.kartify.api.shared.helper.SlugUtil;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorage fileStorage;

    public ProductService(
        ProductRepository productRepository, 
        ProductVariantRepository productVariantRepository,
        ProductAttributeValueRepository productAttributeValueRepository,
        CategoryRepository categoryRepository,
        FileStorage fileStorage
    ){
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorage = fileStorage;
    }

    // --- Create Product ---
    @Transactional
    public ProductResponse create(ProductCreateRequest payload){
        
        Category category = categoryRepository.findById(payload.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // --- Check if product with same sku already exist ---
        if(productRepository.existsBySku(payload.sku())){
            throw new FieldValidationException("sku", "SKU is already exists.");
        }

        Product product = new Product();

        product.setCategory(category);
        product.setName(payload.name());
        product.setSlug(resolveSlug(payload.slug(), payload.name()));
        product.setDescription(payload.description());
        product.setShortDescription(payload.shortDescription());
        product.setSku(payload.sku());
        product.setPrice(payload.price());
        product.setComparePrice(payload.comparePrice());
        product.setCostPrice(payload.costPrice());
        product.setHasVariants(payload.hasVariants());
        product.setStockQuantity(payload.stockQuantity());
        product.setWeight(payload.weight());
        product.setIsActive(payload.isActive());
        product.setIsFeatured(payload.isFeatured());

        // --- Upload product files ---
        if(payload.images() != null && !payload.images().isEmpty()){

            List<UploadedFileResponse> files = uploadFiles(payload.images());

            for (UploadedFileResponse file : files) {

                ProductFile productFile = new ProductFile();
                productFile.setFilename(file.fileName());
                productFile.setName(file.originalName());
                productFile.setSize(file.size());
                productFile.setExtension(file.extension());
                productFile.setMimeType(file.mimeType());

                product.addFile(productFile);

            }
        }

        // --- Save product variant ---
        if(Boolean.TRUE.equals(payload.hasVariants()) && payload.variants() != null && !payload.variants().isEmpty()){
            int index = 0;
            for (ProductVariantRequest productVariant : payload.variants()) {
                ProductVariant variant = this.createVariant(product, productVariant, index);
                product.addVariant(variant);
                index++;
            }
        }

        Product productCreated = productRepository.save(product);

        return toResponse(productCreated);
    }

    // --- Create product variant ---
    private ProductVariant createVariant(Product product, ProductVariantRequest payload, int index){

        // --- Check if product with same sku already exist ---
        if(productVariantRepository.existsBySku(payload.sku())){
            throw new FieldValidationException("variants[" + index + "].sku", "SKU is already exists.");
        }

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku(payload.sku());
        productVariant.setPrice(payload.price());
        productVariant.setStockQuantity(payload.stockQuantity());
        productVariant.setIsActive(payload.isActive());

        // Batch fetch attribute values to avoid N+1 queries
        if (payload.attributeValueIds() != null && !payload.attributeValueIds().isEmpty()) {
            List<ProductAttributeValue> attributeValues = productAttributeValueRepository.findAllById(payload.attributeValueIds());
            
            if (attributeValues.size() != payload.attributeValueIds().size()) {
                throw new ResourceNotFoundException("One or more attribute values were not found.");
            }

            for (ProductAttributeValue attributeValue : attributeValues) {
                productVariant.addVariantAttributeValue(attributeValue);
            }
        }

        return productVariant;

    }

    // --- Upload files to file storage ---
    private List<UploadedFileResponse> uploadFiles(List<MultipartFile> files){
        List<UploadedFileResponse> filesMetadata = new ArrayList<>();
        for (MultipartFile file : files) {
            UploadedFileResponse uploadedFile = fileStorage.upload(file, "product");
            filesMetadata.add(uploadedFile);
        }
        return filesMetadata;
    }

    // --- Get Product Image by Filename ---
    public Resource getImageByFilename(String filename) throws IOException{
        String productPath = "product/" + filename;
        return fileStorage.loadAsResource(productPath);
    }

    // --- Helper Function ---

    private ProductResponse toResponse(Product product){
        return new ProductResponse(
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getShortDescription(),
            product.getSku(),
            product.getPrice(),
            product.getComparePrice(),
            product.getCostPrice(),
            product.getHasVariants(),
            product.getStockQuantity(),
            product.getWeight(),
            product.getIsActive(),
            product.getIsFeatured()
        );
    }

    // --- Generate unique slug ---
    private String generateUniquesSlug(String name){
        String baseSlug = SlugUtil.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (productRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    // --- Resolve slug ---
    private String resolveSlug(String slug, String name) {
        String base = (slug != null && slug.isBlank()) ? slug : name;
        return generateUniquesSlug(base);
    }

}

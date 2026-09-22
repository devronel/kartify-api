package com.kartify.api.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.category.entity.Category;
import com.kartify.api.category.repository.CategoryRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.dto.ProductAdminListResponse;
import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.dto.ProductEditFileResponse;
import com.kartify.api.product.dto.ProductEditResponse;
import com.kartify.api.product.dto.ProductEditVariantResponse;
import com.kartify.api.product.dto.ProductFileListResponse;
import com.kartify.api.product.dto.ProductFileRequest;
import com.kartify.api.product.dto.ProductFileResponse;
import com.kartify.api.product.dto.ProductResponse;
import com.kartify.api.product.dto.ProductUpdateFileRequest;
import com.kartify.api.product.dto.ProductUpdateRequest;
import com.kartify.api.product.dto.ProductVariantRequest;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductAttributeValue;
import com.kartify.api.product.entity.ProductFile;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductAttributeValueRepository;
import com.kartify.api.product.repository.ProductFileRepository;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.shared.dto.PaginationResponse;
import com.kartify.api.shared.dto.UploadedFileResponse;
import com.kartify.api.shared.helper.SlugUtil;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductFileRepository productFileRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorage fileStorage;

    public ProductService(
        ProductRepository productRepository, 
        ProductFileRepository productFileRepository,
        ProductVariantRepository productVariantRepository,
        ProductAttributeValueRepository productAttributeValueRepository,
        CategoryRepository categoryRepository,
        FileStorage fileStorage
    ){
        this.productRepository = productRepository;
        this.productFileRepository = productFileRepository;
        this.productVariantRepository = productVariantRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorage = fileStorage;
    }

    // --- Get all Product with Pagination ---
    public PaginationResponse<ProductAdminListResponse> getAll(String search, Pageable pageable){

        // Get all the paginated products
        Page<Product> productPage;

        if(search == null || search.isBlank()){
            productPage = productRepository.findAll(pageable);
        }else{
            productPage = productRepository.search(search, pageable);
        }

        // Get paginated product content
        List<Product> products = productPage.getContent();

        // Get product ids
        List<Long> productIds = products.stream().map(product -> product.getId()).toList();

        // Get the files by product ids
        List<ProductFileListResponse> primaryImages = productFileRepository.findPrimaryImagesByProductIds(productIds)
            .stream().map(file -> new ProductFileListResponse(
                file.getId(),
                file.getProduct().getId(),
                file.getFilename(),
                file.getName(),
                file.getSize(),
                file.getExtension(),
                file.getMimeType(),
                file.getIsPrimary()
            )).toList();

        // Group Product Image by product id
        Map<Long, ProductFileListResponse> primaryImageByProduct = primaryImages.stream()
            .collect(Collectors.toMap(
                file -> file.productId(),
                file -> file
            ));

        List<ProductAdminListResponse> productLists =  products.stream().map(product -> {
            
                ProductFileListResponse primaryImage = primaryImageByProduct.get(product.getId());

                String filename = primaryImage != null ? primaryImage.filename() : null;

                String primaryImageUrl = fileStorage.getUrl("files/public/product/images/" + filename);
                
                return new ProductAdminListResponse(
                    product.getId(),
                    product.getCategory().getName(),
                    product.getName(),
                    product.getSku(),
                    product.getPrice(),
                    product.getComparePrice(),
                    product.getCostPrice(),
                    product.getHasVariants(),
                    product.getStockQuantity(),
                    product.getWeight(),
                    product.getIsActive(),
                    product.getIsFeatured(),
                    primaryImageUrl
                );   
            }).toList();

        return new PaginationResponse<>(
            productLists,
            productPage.getNumber() + 1,
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.hasNext(),
            productPage.hasPrevious()
        );
    }


    // --- Get product by id including its files and variants ---
    public ProductEditResponse getById(Long id){
        
        // 1. Find the products
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 2. Find the product images
        List<ProductEditFileResponse> files = product.getFiles().stream()
            .map(file -> {

                String imageUrl = fileStorage.getUrl("files/public/product/images/" + file.getFilename());
                
                ProductEditFileResponse productEditFileResponse = new ProductEditFileResponse(
                    file.getId(),
                    imageUrl,
                    file.getIsPrimary()  
                );
                
                return productEditFileResponse;
                
            }).toList();

        // 3. Find Product Variants
        List<ProductVariant> productVariants = productVariantRepository.findByProductIdWithAttributeValues(id);

        List<ProductEditVariantResponse> variants = productVariants.stream()
            .map(variant -> {

                List<Long> attributeValueIds = variant.getAttributeValues().stream()
                    .map(attributeValue -> attributeValue.getId())
                    .toList();

                return new ProductEditVariantResponse(
                    variant.getId(),
                    attributeValueIds,
                    variant.getSku(),
                    variant.getPrice(),
                    variant.getComparePrice(),
                    variant.getCostPrice(),
                    variant.getStockQuantity(),
                    variant.getWeight(),
                    variant.getIsActive()
                );
            })
            .toList();

        return new ProductEditResponse(
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
            product.getIsFeatured(),
            files,
            variants
        );
        
    }


    // --- Create Product ---
    @Transactional
    public ProductResponse create(ProductCreateRequest payload){
        
        Category category = categoryRepository.findById(payload.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // --- Check for duplicate SKU, only if one was provided ---
        if (productRepository.existsBySku(payload.sku())) {
            throw new FieldValidationException("sku", "SKU is already exists.");
        }

        Product product = new Product();

        product.setCategory(category);
        product.setName(payload.name());
        product.setSlug(resolveSlug(payload.slug(), payload.name()));
        
        if(payload.description() != null && !payload.description().isBlank()){
            product.setDescription(payload.description());
        }

        if(payload.shortDescription() != null && !payload.shortDescription().isBlank()){
            product.setShortDescription(payload.shortDescription());
        }

        if(payload.sku() != null && !payload.sku().isBlank()){
            product.setSku(payload.sku());
        }

        product.setPrice(payload.price());

        BigDecimal comparePrice = payload.comparePrice();
        if (comparePrice != null && comparePrice.compareTo(BigDecimal.ZERO) > 0) {
            product.setComparePrice(comparePrice);
        }

        BigDecimal costPrice = payload.costPrice();
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) > 0) {
            product.setCostPrice(costPrice);
        }

        product.setHasVariants(payload.hasVariant());
        product.setStockQuantity(payload.stockQuantity());
        product.setWeight(payload.weight());
        product.setIsActive(payload.isActive());
        product.setIsFeatured(payload.isFeatured());

        // --- Upload product files ---
        if(payload.files() != null && !payload.files().isEmpty()){

            List<ProductFileResponse> files = uploadFiles(payload.files());

            for (ProductFileResponse file : files) {

                UploadedFileResponse metadata = file.file();

                ProductFile productFile = new ProductFile();
                productFile.setFilename(metadata.fileName());
                productFile.setName(metadata.originalName());
                productFile.setSize(metadata.size());
                productFile.setExtension(metadata.extension());
                productFile.setMimeType(metadata.mimeType());
                productFile.setIsPrimary(file.isPrimary());

                product.addFile(productFile);

            }
        }

        // --- Save product variant ---
        if(Boolean.TRUE.equals(payload.hasVariant()) && payload.variants() != null && !payload.variants().isEmpty()){
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


    // --- Update Product including files and variants ---
    @Transactional
    public void update(Long id, ProductUpdateRequest payload){

        // 1. Get the product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No product found"));

        // Get category
        Category category = categoryRepository.findById(payload.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("No category found"));

        // 3. Populate the product details
        product.setCategory(category);
        product.setName(payload.name());
        product.setSlug(payload.slug());
        product.setDescription(payload.description());
        product.setShortDescription(payload.shortDescription());
        product.setSku(payload.sku());
        product.setPrice(payload.price());
        product.setComparePrice(payload.comparePrice());
        product.setCostPrice(payload.costPrice());
        product.setHasVariants(payload.hasVariant());
        product.setStockQuantity(payload.stockQuantity());
        product.setWeight(payload.weight());

        // ------------- START MANAGE IMAGES -------------

        if (payload.files() != null) {

            // 1. Get existing image IDs
            Set<Long> existingProductFileIds = product.getFiles().stream()
                .map(file -> file.getId())
                .collect(Collectors.toSet());

            // 2. Get existing image IDs from the request
            Set<Long> upcomingProductFileIds = payload.files().stream()
                .map(file -> file.id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // 3. Find images that were removed
            /*
                [1, 2, 3] --> existing file
                [1, 3] --> upcoming/updated from the request
                [2] --> Remove ids
            */
            Set<Long> removeIds = new HashSet<>(existingProductFileIds);
            removeIds.removeAll(upcomingProductFileIds);

            // 4. Delete the images
            productFileRepository.deleteByIds(removeIds);

            // 5. If there are images, validate/update/add them
            if (!payload.files().isEmpty()) {

                long primaryCount = payload.files().stream()
                    .filter(file -> Boolean.TRUE.equals(file.isPrimary()))
                    .count();

                if (primaryCount != 1) {
                    throw new IllegalArgumentException("Only one primary file is allowed");
                }

                for (ProductUpdateFileRequest file : payload.files()) {

                    if (file.id() != null) {

                        ProductFile productFile = productFileRepository
                            .findByIdAndProductId(file.id(), product.getId())
                            .orElseThrow(() ->
                                new ResourceNotFoundException("No product file found")
                            );

                        productFile.setIsPrimary(file.isPrimary());

                    } else {

                        UploadedFileResponse metadata = fileStorage.upload(file.file(), "product");

                        ProductFile productFile = new ProductFile();
                        productFile.setFilename(metadata.fileName());
                        productFile.setName(metadata.originalName());
                        productFile.setSize(metadata.size());
                        productFile.setExtension(metadata.extension());
                        productFile.setMimeType(metadata.mimeType());
                        productFile.setIsPrimary(file.isPrimary());

                        product.addFile(productFile);
                    }
                }
            }
        }

        // ------------- END MANAGE IMAGES -------------

        // ------------- START MANAGE VARIANTS -------------

        
        // ------------- END MANAGE VARIANTS -------------

    }


    // --- Create product variant ---
    private ProductVariant createVariant(Product product, ProductVariantRequest payload, int index){

        // --- Check if product with same sku already exist ---
        if(productVariantRepository.existsBySku(payload.sku())){
            throw new FieldValidationException("variants[" + index + "].sku", "SKU is already exists.");
        }

        ProductVariant productVariant = new ProductVariant();
        
        productVariant.setProduct(product);

        if(payload.sku() != null && !payload.sku().isBlank()){
            productVariant.setSku(payload.sku());
        }

        productVariant.setPrice(payload.price());

        BigDecimal comparePrice = payload.comparePrice();
        if (comparePrice != null && comparePrice.compareTo(BigDecimal.ZERO) > 0) {
            productVariant.setComparePrice(comparePrice);
        }

        BigDecimal costPrice = payload.costPrice();
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) > 0) {
            productVariant.setCostPrice(costPrice);
        }

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
    private List<ProductFileResponse> uploadFiles(List<ProductFileRequest> files){
        List<ProductFileResponse> filesMetadata = new ArrayList<>();
        
        for (ProductFileRequest file : files) {
            UploadedFileResponse uploadedFile = fileStorage.upload(file.file(), "product");
            filesMetadata.add(new ProductFileResponse(
                uploadedFile,
                file.isPrimary()
            ));
        }

        return filesMetadata;
    }


    // --- Get Product Image by Filename ---
    public Resource getImageByFilename(String filename){
        try {
            String productPath = "product/" + filename;
            return fileStorage.loadAsResource(productPath);
        } catch (RuntimeException e) {
            return new ClassPathResource(
                "static/images/default.jpg"
            ); 
        }
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

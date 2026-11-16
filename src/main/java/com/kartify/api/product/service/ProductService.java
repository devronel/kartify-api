package com.kartify.api.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.category.entity.Category;
import com.kartify.api.category.repository.CategoryRepository;
import com.kartify.api.category.service.CategoryService;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.dto.CategoryFilterResponse;
import com.kartify.api.product.dto.FeaturedCategoryResponse;
import com.kartify.api.product.dto.ProductAdminListResponse;
import com.kartify.api.product.dto.ProductAttributeValueResponse;
import com.kartify.api.product.dto.ProductAttributeWithValueResponse;
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
import com.kartify.api.product.dto.ProductUpdateVariantRequest;
import com.kartify.api.product.dto.ProductVariantRequest;
import com.kartify.api.product.dto.PublicProductDetailsResponse;
import com.kartify.api.product.dto.PublicProductResponse;
import com.kartify.api.product.dto.PublicVariantResponse;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductAttributeValue;
import com.kartify.api.product.entity.ProductFile;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductAttributeValueRepository;
import com.kartify.api.product.repository.ProductFileRepository;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductSpecifications;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.shared.dto.PaginationResponse;
import com.kartify.api.shared.dto.UploadedFileResponse;
import com.kartify.api.shared.helper.SlugUtil;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductFileRepository productFileRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final FileStorage fileStorage;

    public ProductService(
        ProductRepository productRepository, 
        ProductFileRepository productFileRepository,
        ProductVariantRepository productVariantRepository,
        ProductAttributeValueRepository productAttributeValueRepository,
        CategoryRepository categoryRepository,
        CategoryService categoryService,
        FileStorage fileStorage
    ){
        this.productRepository = productRepository;
        this.productFileRepository = productFileRepository;
        this.productVariantRepository = productVariantRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.fileStorage = fileStorage;
    }

    
    /**
     * Retrieves a paginated list of products for the admin site.
     *
     * Includes product details, primary images, and total stock quantities.
     * Supports keyword-based searching.
     *
     * @param search the keyword used to filter products
     * @param pageable pagination and sorting information
     * @return paginated product information for the admin product list
     */
    public PaginationResponse<ProductAdminListResponse> getAll(String search, Pageable pageable){

        Specification<Product> spec = null;

        if (search != null && !search.isBlank()) {
            spec = ProductSpecifications.keyword(search);
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        List<Product> products = productPage.getContent();

        List<Long> productIds = products.stream().map(product -> product.getId()).toList();

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


        // Group primary images by product ID.
        Map<Long, ProductFileListResponse> primaryImageByProduct = primaryImages.stream()
            .collect(Collectors.toMap(
                file -> file.productId(),
                file -> file
            ));


        // Map total variant stock quantities by product ID.
        Map<Long, Integer> variantStockByProduct = productVariantRepository
            .sumStockByProductIds(productIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Number) row[1]).intValue()
            ));
        

        List<ProductAdminListResponse> productLists =  products.stream().map(product -> {
            
                ProductFileListResponse primaryImage = primaryImageByProduct.get(product.getId());

                String filename = primaryImage != null ? primaryImage.filename() : null;

                String primaryImageUrl = fileStorage.getUrl("files/public/product/images/" + filename);
                
                Integer stockQuantity = !product.getHasVariants() 
                    ? product.getStockQuantity() 
                    : variantStockByProduct.getOrDefault(product.getId(), 0);

                return new ProductAdminListResponse(
                    product.getId(),
                    product.getCategory().getName(),
                    product.getName(),
                    product.getSku(),
                    product.getPrice(),
                    product.getComparePrice(),
                    product.getCostPrice(),
                    product.getHasVariants(),
                    stockQuantity,
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


    /**
     * Retrieves a product by its ID for the admin product editing page.
     *
     * Includes product details, associated files, variants, and their
     * corresponding attribute values.
     *
     * @param id the ID of the product to retrieve
     * @return the product edit response containing all required product information
     * @throws ResourceNotFoundException if the product does not exist
    */
    public ProductEditResponse getById(Long id){
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));


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


        // Retrieve product variants with their associated attribute values.
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


        // Group variant attribute values by product attribute.
        List<ProductAttributeWithValueResponse> variantAttributes = productVariants.stream()
            .flatMap(variant -> {
                return variant.getAttributeValues().stream();
            })
            .collect(Collectors.groupingBy(
                attributeValue -> attributeValue.getProductAttribute().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet().stream()
            .map(entry -> {
                List<ProductAttributeValueResponse> values = entry.getValue().stream()
                    .map(attributeValue -> new ProductAttributeValueResponse(
                        attributeValue.getId(),
                        attributeValue.getProductAttribute().getId(),
                        attributeValue.getValue()
                    ))
                    .distinct()
                    .toList();

                String attributeName = entry.getValue().get(0).getProductAttribute().getName();

                ProductAttributeWithValueResponse response = new ProductAttributeWithValueResponse(
                    entry.getKey(),
                    attributeName,
                    values
                );

                return response;
            })
            .toList();

        return new ProductEditResponse(
            product.getCategory().getId(),
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
            files,
            variants,
            variantAttributes
        );
        
    }


    /**
     * Create new product
     * 
     * Includes product details, associated files, variants and its corresponding
     * attribute values
     * 
     * @param payload containing all the required product information
     * @return product response containing some of the newly added created product
     * @throws ResourceNotFoundException if no category found
     * @throws FieldValidationException if product sku already exists
     */
    @Transactional
    public ProductResponse create(ProductCreateRequest payload){
        
        Category category = categoryRepository.findById(payload.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check for duplicate SKU, only if one was provided
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
        product.setIsActive(false);
        product.setIsFeatured(false);


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


    /*
        TODO: When hasVariants changes in either direction, delete that product’s cart items.
    */
   /**
    * Save updated products
    * 
    * Includes product details, associated files, variants and its corresponding
    * attribute values
    * 
    * @param id
    * @param payload contains all the update product details
    * @return true if the update is success 
    * @throws ResourceNotFoundException if product doesn't exists
    * @throws ResourceNotFoundException if category doesn't exists
    * @throws IllegalArgumentException if product primary images is more than one
    * @throws ResourceNotFoundException if theres file id in the request but not exist's in the db
    * @throws ResourceNotFoundException if theres variant id in the request but not exist's in the db
    */
    @Transactional
    public boolean update(Long id, ProductUpdateRequest payload){

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No product found"));

        Category category = categoryRepository.findById(payload.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("No category found"));

        product.setCategory(category);
        product.setName(payload.name());
        product.setSlug(payload.slug());
        product.setDescription(payload.description());
        product.setShortDescription(payload.shortDescription());
        product.setSku(!payload.sku().isBlank() ? payload.sku().toUpperCase() : null);
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
        product.setIsActive(false);
        product.setIsFeatured(false);
        product.setWeight(payload.weight());


        // START MANAGE IMAGES
        if(payload.files() == null) {
            product.getFiles().clear();
        } else {

            Set<Long> existingProductFileIds = product.getFiles().stream()
                .map(file -> file.getId())
                .collect(Collectors.toSet());
    
            Set<Long> incomingProductFileIds = payload.files().stream()
                .map(file -> file.id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    
            // Find images that were removed
            Set<Long> removeIds = new HashSet<>(existingProductFileIds);
            removeIds.removeAll(incomingProductFileIds);
    
            product.getFiles().removeIf(file -> removeIds.contains(file.getId()));
    
            // If there are images, validate, update existing and add if theres new image found
            if (!payload.files().isEmpty()) {
    
                long primaryCount = payload.files().stream()
                    .filter(file -> Boolean.TRUE.equals(file.isPrimary()))
                    .count();
    
                if (primaryCount != 1) {
                    throw new IllegalArgumentException("Product must have exactly one primary file");
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


        // START MANAGE VARIANTS
        if(Boolean.FALSE.equals(payload.hasVariant())){
            product.getVariants().clear();
        }else{

            Set<Long> existingVariantIds = product.getVariants().stream()
                .map(variant -> variant.getId())
                .collect(Collectors.toSet());
    
            Set<Long> upcomingVariantIds = payload.variants().stream()
                .map(variant -> variant.id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    
            // Get variants ids to be remove
            Set<Long> removeVariantIds = new HashSet<>(existingVariantIds);
            removeVariantIds.removeAll(upcomingVariantIds);
    
            product.getVariants().removeIf(variant -> removeVariantIds.contains(variant.getId()));
            
            Set<Long> attributeValueIds = payload.variants().stream()
                .flatMap(variant -> variant.attributeValueIds().stream())
                .collect(Collectors.toSet());

            List<ProductAttributeValue> productAttributeValues = productAttributeValueRepository.findAllById(attributeValueIds);

            Map<Long, ProductAttributeValue> productAttributeValueMap = productAttributeValues.stream()
                .collect(Collectors.toMap(value -> value.getId(), Function.identity()));

            // Process incoming variants
            for (ProductUpdateVariantRequest payloadVariant : payload.variants()) {
    
                ProductVariant productVariant;
    
                if (payloadVariant.id() != null) {
                    productVariant = product.getVariants().stream()
                        .filter(variant -> variant.getId().equals(payloadVariant.id()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
                } else {
                    productVariant = new ProductVariant();
                    product.addVariant(productVariant);
                }
    
                productVariant.getAttributeValues().clear();
    
                for (Long attributeValueId : payloadVariant.attributeValueIds()) {
    
                    ProductAttributeValue attributeValue = productAttributeValueMap.get(attributeValueId);

                    if (attributeValue == null) {
                        throw new ResourceNotFoundException("Variant attribute value not found");
                    }

                    productVariant.addVariantAttributeValue(attributeValue);
                }
    
                productVariant.setSku(!payloadVariant.sku().isBlank() ? payloadVariant.sku().toUpperCase() : null);
                productVariant.setPrice(payloadVariant.price());

                BigDecimal variantComparePrice = payloadVariant.comparePrice();
                if (variantComparePrice != null && variantComparePrice.compareTo(BigDecimal.ZERO) > 0) {
                    productVariant.setComparePrice(variantComparePrice);
                }

                BigDecimal variantCostPrice = payloadVariant.costPrice();
                if (variantCostPrice != null && variantCostPrice.compareTo(BigDecimal.ZERO) > 0) {
                    productVariant.setCostPrice(variantCostPrice);
                }

                productVariant.setStockQuantity(payloadVariant.stockQuantity());
                productVariant.setWeight(payloadVariant.weight());
                productVariant.setIsActive(payloadVariant.isActive());
            }

        }

        return true;
    }


    /**
    * Update product `active` properties
    * 
    * Switch the product from active to inactive and vice-versa
    * 
    * @param id
    * @param active contains boolean value
    * @return void
    * @throws ResourceNotFoundException if product doesn't exists
    */
    public void updateActive(Long id, boolean active) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setIsActive(active);

        productRepository.save(product);

    }


    /**
     * Delete product
     * 
     * @param id
     * @return void
     * @throws ResourceNotFoundException if product doesn't exists
     */
    public void delete(Long id) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productRepository.delete(product);

    }


    /**
     * Creates a product variant and associates it with the specified product.
     *
     * Validates SKU uniqueness, assigns pricing and stock information,
     * and associates the selected attribute values.
     *
     * @param product the product associated with the variant
     * @param payload the variant creation details
     * @param index the variant index used to identify validation errors
     * @return the newly created product variant
     * @throws FieldValidationException if the SKU is already assigned to another variant
     * @throws ResourceNotFoundException if one or more attribute values do not exist
    */
    private ProductVariant createVariant(Product product, ProductVariantRequest payload, int index){

        // Ensure the SKU is unique across product variants.
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

        // Fetch attribute values in a single query to avoid N+1 queries.
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


    /**
     * Upload file to file storage
     * 
     * 
     * @param files
     * @return list of product file response
     */
    private List<ProductFileResponse> uploadFiles(List<ProductFileRequest> files){
        List<ProductFileResponse> filesMetadata = new ArrayList<>();
        
        for (ProductFileRequest file : files) {
            UploadedFileResponse uploadedFile = fileStorage.upload(file.file(), "product");
            filesMetadata.add(new ProductFileResponse(uploadedFile,file.isPrimary()));
        }

        return filesMetadata;
    }


    /**
     * Get file by filename
     * 
     * Load image as resource from file storage and if failed
     * it has fallback image
     * 
     * @param filename
     * @return resource for the actual file or image
     */
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


    // --------------- Public --------------------

    /**
     * Retrieves a paginated list of active products for the storefront.
     *
     * Supports keyword searching and category filtering, including products
     * belonging to child categories.
     *
     * Includes product details and primary images.
     *
     * @param search the keyword used to filter products
     * @param categorySlug the category slug used to filter products and their descendants
     * @param pageable pagination and sorting information
     * @return paginated active product information
     * @throws ResourceNotFoundException if the specified category does not exist
    */
    public PaginationResponse<PublicProductResponse> getAllActive(String search, String categorySlug, Pageable pageable){

        Specification<Product> spec = Specification.where(ProductSpecifications.isActive());

        if (search != null && !search.isBlank()) {
            spec = spec.and(ProductSpecifications.keyword(search));
        }

        if (categorySlug != null && !categorySlug.isBlank()) {

            Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            List<Long> categoryIds = categoryService.collectCategoryAndChildIds(category);

            spec = spec.and(ProductSpecifications.hasCategories(categoryIds));
        }

        // Retrieve the paginated products matching the search criteria.
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Product> products = productPage.getContent();

        List<Long> productIds = products.stream().map(product -> product.getId()).toList();


        // Retrieve primary images for all products in the current page.
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


        // Group primary images by product ID for efficient lookup.
        Map<Long, ProductFileListResponse> primaryImageByProduct = primaryImages.stream()
            .collect(Collectors.toMap(
                file -> file.productId(),
                file -> file
            ));
        

        List<PublicProductResponse> productLists =  products.stream().map(product -> {
            
                ProductFileListResponse primaryImage = primaryImageByProduct.get(product.getId());

                String filename = primaryImage != null ? primaryImage.filename() : null;

                String primaryImageUrl = fileStorage.getUrl("files/public/product/images/" + filename);
                
                return new PublicProductResponse(
                    product.getId(),
                    product.getCategory().getName(),
                    product.getName(),
                    product.getSlug(),
                    product.getDescription(),
                    product.getShortDescription(),
                    product.getPrice(),
                    product.getComparePrice(),
                    product.getHasVariants(),
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


    /**
     * Retrieves the details of an active product using its slug.
     *
     * Includes product information, images, available variants, variant attributes,
     * and total stock quantity.
     *
     * For products with variants, retrieves their associated attribute values
     * and calculates the total stock across all variants.
     *
     * @param slug the unique slug of the product
     * @return the public product details response
     * @throws ResourceNotFoundException if the product does not exist or is inactive
    */
    public PublicProductDetailsResponse getProductBySlug(String slug){

        Product product = productRepository.findBySlug(slug)
            .filter(prod -> prod.getIsActive())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<PublicVariantResponse> variants = new ArrayList<>();
        List<ProductAttributeWithValueResponse> variantAttributes = new ArrayList<>();
        
        // Retrieve product variants with their associated attribute values.
        if(Boolean.TRUE.equals(product.getHasVariants())){

            List<ProductVariant> productVariants = productVariantRepository.findByProductIdWithAttributeValues(product.getId());

            variants = productVariants.stream()
                .map(variant -> {

                    List<Long> attributeValueIds = variant.getAttributeValues().stream()
                        .map(attributeValue -> attributeValue.getId())
                        .toList();

                    Boolean inStock = variant.getStockQuantity() > 0;

                    return new PublicVariantResponse(
                        variant.getId(),
                        attributeValueIds,
                        variant.getSku(),
                        variant.getPrice(),
                        inStock,
                        variant.getStockQuantity()
                    );
                })
                .toList();

            // Group variant attribute values by product attribute.
            variantAttributes = productVariants.stream()
                .flatMap(variant -> variant.getAttributeValues().stream())
                .collect(Collectors.groupingBy(
                    attributeValue -> attributeValue.getProductAttribute().getId(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {

                    List<ProductAttributeValueResponse> values = entry.getValue().stream()
                        .map(attributeValue -> new ProductAttributeValueResponse(
                            attributeValue.getId(),
                            attributeValue.getProductAttribute().getId(),
                            attributeValue.getValue()
                        ))
                        .distinct()
                        .toList();

                    String attributeName = entry.getValue().get(0).getProductAttribute().getName();

                    ProductAttributeWithValueResponse response = new ProductAttributeWithValueResponse(
                        entry.getKey(),
                        attributeName,
                        values
                    );

                    return response;
                })
                .toList();
        }


        // Retrieve all product image URLs.
        List<String> images = product.getFiles().stream()
            .map(file -> fileStorage.getUrl("files/public/product/images/" + file.getFilename()))
            .toList();


        // Calculate total stock based on whether the product has variants.
        Integer stockQuantity = !product.getHasVariants() 
                ? product.getStockQuantity() 
                : product.getVariants().stream()
                    .mapToInt(variant -> variant.getStockQuantity())
                    .sum();


        return new PublicProductDetailsResponse(
            product.getId(),
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getShortDescription(),
            product.getPrice(),
            product.getComparePrice(),
            product.getHasVariants(),
            true,
            stockQuantity,
            images,
            variantAttributes,
            variants
        );

    }


    /**
     * Retrieves top-level categories for the storefront product filters.
     *
     * @return a list of category filter responses
    */
    public List<CategoryFilterResponse> getCategoryFiltersForStorefront() {
        List<Category> topLevelCategories = categoryRepository.findAllTopLevel();
        return topLevelCategories.stream()
            .map(this::toCategoryFilterResponse)
            .toList();
    }


    /**
     * Retrieves featured top-level categories for the storefront.
     *
     * Calculates the total number of products in each category, including
     * products belonging to its child categories. Excludes categories
     * without products and sorts the results by product count in descending order.
     *
     * @param limit the maximum number of categories to return (minimum 1, maximum 12)
     * @return a list of featured categories sorted by product count
    */
    public List<FeaturedCategoryResponse> getFeaturedCategories(int limit){
        
        // Ensure the requested limit is within the allowed range.
        int safeLimit = Math.min(Math.max(limit, 1), 12);

        List<Object[]> productCountByCategory = productRepository.countProductsByCategory();

        // Map direct product counts by category ID for efficient lookup.
        Map<Long, Long> direct = productCountByCategory.stream()
            .collect(Collectors.toMap(categoryId -> (Long) categoryId[0], productCount -> (Long) productCount[1]));

        return categoryRepository.findAllTopLevel()
            .stream()
            .map(category -> {

                // Include products from the category and all its descendants.
                long total = collectCategoryAndChildIds(category).stream()
                    .mapToLong(id -> direct.getOrDefault(id, 0L))
                    .sum();

                return new FeaturedCategoryResponse(category.getId(), category.getName(), total);
            })
            .filter(category -> category.productCount() > 0)
            .sorted(Comparator.comparingLong((FeaturedCategoryResponse category) -> category.productCount()).reversed()
                .thenComparing(category -> category.name(), String.CASE_INSENSITIVE_ORDER))
            .limit(safeLimit)
            .toList();
    }

    // --------------- Public --------------------


    
    // ---------------- Helper Function ----------------

    public List<Long> collectCategoryAndChildIds(Category category) {
        List<Long> ids = new ArrayList<>();
        
        ids.add(category.getId());

        for (Category child : category.getChildren()) {
            ids.addAll(collectCategoryAndChildIds(child));
        }

        return ids;
    }


    private CategoryFilterResponse toCategoryFilterResponse(Category category) {
        List<CategoryFilterResponse> children = category.getChildren().stream()
            .map(this::toCategoryFilterResponse)
            .toList();

        List<Long> descendantIds = categoryService.collectCategoryAndChildIds(category);
        Integer cumulativeCount = productRepository.countByCategoryIdIn(descendantIds);

        return new CategoryFilterResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            cumulativeCount,
            children
        );
    }


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

    
    private String resolveSlug(String slug, String name) {
        String base = (slug != null && slug.isBlank()) ? slug : name;
        return generateUniquesSlug(base);
    }


    // ---------------- Helper Function ----------------
}

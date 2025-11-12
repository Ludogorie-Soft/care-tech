package com.techstore.service.admin;

import com.techstore.dto.request.ProductPromoRequest;
import com.techstore.dto.response.*;
import com.techstore.entity.*;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ParameterMapper parameterMapper;
    private final CacheManager cacheManager;

    public List<ProductResponseDTO> createPromoByManufacturer(Long manufacturerId, BigDecimal discount, String lang) {
        Manufacturer manufacturer = manufacturerRepository.findById(manufacturerId).orElseThrow(
                () -> new ResourceNotFoundException("Manufacturer not found with id " + manufacturerId)
        );

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            manufacturer.setIsPromoActive(true);
        } else {
            manufacturer.setIsPromoActive(false);
        }
        manufacturerRepository.save(manufacturer);

        List<Product> products = productRepository.findByManufacturerId(manufacturerId);

        if (!products.isEmpty()) {
            for (Product product : products) {
                setDiscountToProduct(discount, product);
            }
        }

        clearProductCache();

        return products.stream().map(p -> convertToResponseDTO(p, lang)).toList();
    }

    public List<CategorySummaryDTO> findByIsCategoryPromoActive() {
        return categoryRepository.findByIsPromoActiveTrue().stream()
                .map(this::convertToCategorySummary)
                .toList();
    }

    public List<ManufacturerSummaryDto> findByIsManufacturerPromoActive() {
        return manufacturerRepository.findByIsPromoActiveTrue().stream()
                .map(this::convertToManufacturerSummary)
                .toList();
    }

    public List<ProductResponseDTO> createPromoByCategory(Long categoryId, BigDecimal discount, String language) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with id " + categoryId)
        );

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            category.setIsPromoActive(true);
        } else {
            category.setIsPromoActive(false);
        }

        categoryRepository.save(category);

        List<Product> products = productRepository.findAllByCategoryId(category.getId());

        if (!products.isEmpty()) {
            for (Product product : products) {
                setDiscountToProduct(discount, product);
            }
        }

        clearProductCache();

        return products.stream().map(p -> convertToResponseDTO(p, language)).toList();
    }

    public ProductResponseDTO createPromo(ProductPromoRequest request, String lang) {
        Product product = getProduct(request.getId());

        setDiscountToProduct(request.getDiscount(), product);

        clearProductCache();

        return convertToResponseDTO(product, lang);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with id: " + productId)
        );
    }

    private ProductResponseDTO convertToResponseDTO(Product product, String lang) {
        log.debug("=== CONVERTING PRODUCT TO RESPONSE DTO ===");
        log.debug("Product ID: {}, Language: {}", product.getId(), lang);
        log.debug("ProductParameters count: {}", product.getProductParameters().size());

        ProductResponseDTO dto = new ProductResponseDTO();

        dto.setId(product.getId());
        dto.setNameEn(product.getNameEn());
        dto.setNameBg(product.getNameBg());
        dto.setDescriptionEn(product.getDescriptionEn());
        dto.setDescriptionBg(product.getDescriptionBg());

        dto.setReferenceNumber(product.getReferenceNumber());
        dto.setModel(product.getModel());
        dto.setBarcode(product.getBarcode());

        dto.setPriceClient(product.getPriceClient());
        dto.setPricePartner(product.getPricePartner());
        dto.setPricePromo(product.getPricePromo());
        dto.setPriceClientPromo(product.getPriceClientPromo());
        dto.setMarkupPercentage(product.getMarkupPercentage());
        dto.setFinalPrice(product.getFinalPrice());
        dto.setDiscount(product.getDiscount());

        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setShow(product.getShow());

        if (product.getPrimaryImageUrl() != null) {
            dto.setPrimaryImageUrl("/api/images/product/" + product.getId() + "/primary");
        }

        if (product.getAdditionalImages() != null && !product.getAdditionalImages().isEmpty()) {
            List<String> proxyAdditionalUrls = new ArrayList<>();
            for (int i = 0; i < product.getAdditionalImages().size(); i++) {
                proxyAdditionalUrls.add("/api/images/product/" + product.getId() + "/additional/" + i);
            }
            dto.setAdditionalImages(proxyAdditionalUrls);
        }

        dto.setWarranty(product.getWarranty());
        dto.setWeight(product.getWeight());

        // ========== DETAILED LOGGING FOR SPECIFICATIONS ==========
        log.debug("--- PROCESSING SPECIFICATIONS ---");
        Set<ProductParameter> productParams = product.getProductParameters();
        log.debug("Raw ProductParameter set size: {}", productParams.size());

        // Log each ProductParameter before processing
        int counter = 1;
        for (ProductParameter pp : productParams) {
            log.debug("ProductParam {}: ID={}, Parameter={}, Option={}",
                    counter++, pp.getId(),
                    pp.getParameter() != null ? pp.getParameter().getNameEn() : "NULL",
                    pp.getParameterOption() != null ? pp.getParameterOption().getNameEn() : "NULL");

            if (pp.getParameter() != null) {
                log.debug("  Parameter details: ID={}, ExternalID={}, NameEN={}, NameBG={}",
                        pp.getParameter().getId(),
                        pp.getParameter().getExternalId(),
                        pp.getParameter().getNameEn(),
                        pp.getParameter().getNameBg());
            }

            if (pp.getParameterOption() != null) {
                log.debug("  Option details: ID={}, ExternalID={}, NameEN={}, NameBG={}",
                        pp.getParameterOption().getId(),
                        pp.getParameterOption().getExternalId(),
                        pp.getParameterOption().getNameEn(),
                        pp.getParameterOption().getNameBg());
            }
        }

        // Filter out null parameters/options
        List<ProductParameter> validParams = productParams.stream()
                .filter(pp -> {
                    boolean valid = pp.getParameter() != null && pp.getParameterOption() != null;
                    if (!valid) {
                        log.warn("Filtered out invalid ProductParameter ID: {} (null parameter or option)", pp.getId());
                    }
                    return valid;
                })
                .toList();

        log.debug("Valid ProductParameters after filtering: {}", validParams.size());

        // Process each parameter and log conversion
        Map<Long, ProductParameterResponseDto> uniqueSpecs = new HashMap<>();

        for (ProductParameter pp : validParams) {
            Long parameterId = pp.getParameter().getId();
            log.debug("Processing Parameter ID: {}, Name: {}", parameterId, pp.getParameter().getNameEn());

            // Convert to DTO
            ProductParameterResponseDto converted = convertToProductParameterResponse(pp, lang);

            if (converted == null) {
                log.error("convertToProductParameterResponse returned NULL for Parameter ID: {}", parameterId);
                continue;
            }

            log.debug("Converted DTO: ParameterID={}, Name={}, Options count={}",
                    converted.getParameterId(), converted.getParameterNameEn(),
                    converted.getOptions() != null ? converted.getOptions().size() : 0);

            // Check if we already have this parameter (duplicate handling)
            if (uniqueSpecs.containsKey(parameterId)) {
                log.debug("DUPLICATE Parameter ID found: {}. Merging options...", parameterId);

                ProductParameterResponseDto existing = uniqueSpecs.get(parameterId);
                Set<ParameterOptionResponseDto> combinedOptions = new HashSet<>();
                combinedOptions.addAll(existing.getOptions());
                combinedOptions.addAll(converted.getOptions());

                log.debug("Before merge - Existing options: {}, New options: {}",
                        existing.getOptions().size(), converted.getOptions().size());

                // Remove duplicate options by ID
                Set<ParameterOptionResponseDto> uniqueOptions = combinedOptions.stream()
                        .collect(Collectors.toMap(
                                ParameterOptionResponseDto::getId,
                                option -> option,
                                (o1, o2) -> {
                                    log.debug("Duplicate option ID: {}, keeping first", o1.getId());
                                    return o1;
                                }))
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(ParameterOptionResponseDto::getOrder))
                        .collect(Collectors.toSet());

                existing.setOptions(uniqueOptions);
                log.debug("After merge - Final options count: {}", uniqueOptions.size());

            } else {
                log.debug("Adding new Parameter ID: {} to unique specs", parameterId);
                uniqueSpecs.put(parameterId, converted);
            }
        }

        log.debug("Final uniqueSpecs map size: {}", uniqueSpecs.size());

        // Convert to list and set in DTO
        List<ProductParameterResponseDto> finalSpecs = new ArrayList<>(uniqueSpecs.values());
        log.debug("Final specifications list size: {}", finalSpecs.size());

        // Log final specifications
        for (ProductParameterResponseDto spec : finalSpecs) {
            log.debug("Final Spec: ParameterID={}, Name={}, Options={}",
                    spec.getParameterId(), spec.getParameterNameEn(),
                    spec.getOptions().size());

            for (ParameterOptionResponseDto option : spec.getOptions()) {
                log.debug("  Option: ID={}, Name={}", option.getId(), option.getName());
            }
        }

        dto.setSpecifications(finalSpecs);

        log.debug("=== SPECIFICATIONS PROCESSING COMPLETE ===");
        log.debug("Final DTO specifications count: {}", dto.getSpecifications().size());

        if (product.getCategory() != null) {
            dto.setCategory(convertToCategorySummary(product.getCategory()));
        }

        if (product.getManufacturer() != null) {
            dto.setManufacturer(convertToManufacturerSummary(product.getManufacturer()));
        }

        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setOnSale(product.isOnSale());
        dto.setStatus(product.getStatus() != null ? product.getStatus().getCode() : 0);
        dto.setWorkflowId(product.getWorkflowId());

        log.debug("=== PRODUCT CONVERSION COMPLETE ===");
        return dto;
    }

    private ProductParameterResponseDto convertToProductParameterResponse(ProductParameter productParameter, String lang) {
        log.debug("=== Converting ProductParameter ===");
        log.debug("ProductParameter ID: {}", productParameter.getId());

        if (productParameter.getParameter() == null) {
            log.error("Parameter is NULL for ProductParameter ID: {}", productParameter.getId());
            return null;
        }

        if (productParameter.getParameterOption() == null) {
            log.error("ParameterOption is NULL for ProductParameter ID: {}", productParameter.getId());
            return null;
        }

        Parameter parameter = productParameter.getParameter();
        ParameterOption option = productParameter.getParameterOption();

        log.debug("Parameter: ID={}, External ID={}, Name EN={}, Name BG={}",
                parameter.getId(), parameter.getExternalId(), parameter.getNameEn(), parameter.getNameBg());
        log.debug("Option: ID={}, External ID={}, Name EN={}, Name BG={}",
                option.getId(), option.getExternalId(), option.getNameEn(), option.getNameBg());

        // Използвай ParameterMapper за option
        ParameterOptionResponseDto optionDto = parameterMapper.toOptionResponseDto(option, lang);
        log.debug("Mapped option DTO: ID={}, Name={}", optionDto.getId(), optionDto.getName());

        ProductParameterResponseDto result = ProductParameterResponseDto.builder()
                .parameterId(parameter.getId())
                .parameterNameEn(parameter.getNameEn())
                .parameterNameBg(parameter.getNameBg())
                .options(Set.of(optionDto))  // Само избраната опция
                .build();

        log.debug("Built ProductParameterResponseDto: Parameter ID={}, Options count={}",
                result.getParameterId(), result.getOptions().size());

        return result;
    }

    private CategorySummaryDTO convertToCategorySummary(Category category) {
        return CategorySummaryDTO.builder()
                .id(category.getId())
                .nameEn(category.getNameEn())
                .nameBg(category.getNameBg())
                .slug(category.getSlug())
                .show(category.getShow())
                .isPromoActive(category.getIsPromoActive())
                .build();
    }

    private ManufacturerSummaryDto convertToManufacturerSummary(Manufacturer manufacturer) {
        return ManufacturerSummaryDto.builder()
                .id(manufacturer.getId())
                .name(manufacturer.getName())
                .isPromoActive(manufacturer.getIsPromoActive())
                .build();
    }

    private void clearProductCache() {
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }
    }

    private void setDiscountToProduct(BigDecimal discount, Product product) {
        product.setDiscount(discount);
        product.calculateFinalPrice();
        productRepository.save(product);
    }
}


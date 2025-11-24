package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedLookupService {

    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "parameters", key = "#externalId")
    public Optional<Parameter> getParameter(Long externalId) {
        return parameterRepository.findByExternalId(externalId);
    }

    @Cacheable(value = "parametersByCategory", key = "#category.id")
    public Map<String, Parameter> getParametersByCategory(Category category) {
        return parameterRepository.findByCategoryIdOrderByOrderAsc(category.getId())
                .stream()
                .filter(p -> p.getExternalId() != null)
                .collect(Collectors.toMap(
                        p -> p.getExternalId().toString(),
                        p -> p,
                        (existing, duplicate) -> {
                            log.debug("Duplicate parameter externalId {} for category {}, keeping first",
                                    existing.getExternalId(), category.getId());
                            return existing;
                        }
                ));
    }

    @Cacheable(value = "parameterOptions", key = "#externalId + '-' + #parameterId")
    public Optional<ParameterOption> getParameterOption(Long externalId, Long parameterId) {
        return parameterOptionRepository.findByExternalIdAndParameterId(externalId, parameterId);
    }

    @Cacheable(value = "parameterOptions", key = "#externalId")
    public Optional<ParameterOption> getParameterOption(Long externalId) {
        return parameterOptionRepository.findByExternalId(externalId);
    }

    @Cacheable(value = "categoriesByExternalId")
    public Map<Long, Category> getAllCategoriesMap() {
        return categoryRepository.findAll()
                .stream()
                .filter(c -> c.getExternalId() != null)
                .collect(Collectors.toMap(
                        Category::getExternalId,
                        c -> c,
                        (existing, duplicate) -> {
                            log.warn("Duplicate category externalId: {}, keeping first", existing.getExternalId());
                            return existing;
                        }
                ));
    }

    @Cacheable(value = "manufacturersByExternalId")
    public Map<Long, Manufacturer> getAllManufacturersMap() {
        return manufacturerRepository.findAll()
                .stream()
                .filter(m -> m.getExternalId() != null)
                .collect(Collectors.toMap(
                        Manufacturer::getExternalId,
                        m -> m,
                        (existing, duplicate) -> {
                            log.warn("Duplicate manufacturer externalId: {}, keeping first", existing.getExternalId());
                            return existing;
                        }
                ));
    }

    @Cacheable(value = "productsByCategory", key = "#category.id")
    public Map<Long, Product> getProductsByCategory(Category category) {
        return productRepository.findAllByCategoryId(category.getId())
                .stream()
                .filter(p -> p.getExternalId() != null)
                .collect(Collectors.toMap(
                        Product::getExternalId,
                        p -> p,
                        (existing, duplicate) -> {
                            log.warn("Duplicate product externalId {} for category {}, keeping first",
                                    existing.getExternalId(), category.getId());
                            return existing;
                        }
                ));
    }

    @CacheEvict(value = "parameters", allEntries = true)
    public void clearParametersCache() {
        log.info("Cleared parameters cache");
    }

    @CacheEvict(value = "parametersByCategory", allEntries = true)
    public void clearParametersByCategoryCache() {
        log.info("Cleared parameters by category cache");
    }

    @CacheEvict(value = {"parameters", "parametersByCategory", "parameterOptions"}, allEntries = true)
    public void clearAllParameterCaches() {
        log.info("Cleared all parameter-related caches");
    }
}
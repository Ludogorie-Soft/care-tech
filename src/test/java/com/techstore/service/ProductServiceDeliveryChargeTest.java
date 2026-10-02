package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.enums.DeliveryCharge;
import com.techstore.enums.ProductStatus;
import com.techstore.mapper.ManufacturerMapper;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.filter.DisplaySpecificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * The product page shows the delivery cost under the price, and Pazaruvaj publishes the feed's
 * DeliveryCost only when the two agree. Both come from {@link ShippingConfig#singleProductDelivery};
 * this pins that the product API carries it.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceDeliveryChargeTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ManufacturerRepository manufacturerRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private ParameterOptionRepository parameterOptionRepository;
    @Mock private S3Service s3Service;
    @Mock private ParameterMapper parameterMapper;
    @Mock private DisplaySpecificationService displaySpecificationService;
    @Mock private ManufacturerMapper manufacturerMapper;
    @Mock private ProductParameterRepository productParameterRepository;
    @Mock private CacheManager cacheManager;

    private ProductService service;

    @BeforeEach
    void setUp() {
        ShippingConfig shippingConfig = new ShippingConfig();
        ReflectionTestUtils.setField(shippingConfig, "defaultShippingCost", new BigDecimal("3.50"));
        ReflectionTestUtils.setField(shippingConfig, "freeShippingThreshold", new BigDecimal("170.00"));
        ReflectionTestUtils.setField(shippingConfig, "defaultShippingMaxWeightKg", new BigDecimal("3"));
        ReflectionTestUtils.setField(shippingConfig, "freeShippingMaxWeightKg", new BigDecimal("10"));
        ReflectionTestUtils.setField(shippingConfig, "freeShippingExcludedCategoryIds", Set.of(72L, 73L, 74L, 203L, 139L, 32L));

        service = new ProductService(productRepository, categoryRepository, manufacturerRepository,
                parameterRepository, parameterOptionRepository, s3Service, parameterMapper,
                displaySpecificationService, manufacturerMapper, productParameterRepository,
                cacheManager, shippingConfig);
    }

    @Test
    @DisplayName("Light product under 170 € with VAT: flat rate")
    void cheapLightProduct() {
        assertEquals(DeliveryCharge.FIXED, deliveryChargeOf("100.00", null, 11L)); // 120.00 € with VAT
    }

    @Test
    @DisplayName("Product from 170 € with VAT, up to 10 kg: free")
    void expensiveLightProduct() {
        assertEquals(DeliveryCharge.FREE, deliveryChargeOf("150.00", "5.40", 11L)); // 180.00 € with VAT
    }

    @Test
    @DisplayName("A TV from 170 € with VAT: courier's tariff")
    void excludedCategory() {
        assertEquals(DeliveryCharge.COURIER_TARIFF, deliveryChargeOf("500.00", "14.70", 139L));
    }

    private DeliveryCharge deliveryChargeOf(String netPrice, String weightKg, Long categoryId) {
        Category category = new Category();
        category.setId(categoryId);

        Product p = new Product();
        p.setId(1L);
        p.setSlug("product-1");
        p.setActive(true);
        p.setShow(true);
        p.setStatus(ProductStatus.AVAILABLE);
        p.setFinalPrice(new BigDecimal(netPrice));
        p.setWeight(weightKg != null ? new BigDecimal(weightKg) : null);
        p.setCategory(category);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        ProductResponseDTO dto = service.getProductById(1L, "bg");
        return dto.getDeliveryCharge();
    }
}

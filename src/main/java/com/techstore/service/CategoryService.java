package com.techstore.service;

import com.techstore.dto.CategoryResponseDTO;
import com.techstore.dto.CategorySummaryDTO;
import com.techstore.dto.request.CategoryRequestDto;
import com.techstore.entity.Category;
import com.techstore.entity.SyncLog;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SyncLogRepository syncLogRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findByShowTrueOrderBySortOrderAscNameEnAsc()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findByShowTrue(pageable)
                .map(this::convertToResponseDTO);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        Category category = findById(id);
        return convertToResponseDTO(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryBySlug(String slug) {
        Category category = findBySlug(slug);
        return convertToResponseDTO(category);
    }

    public CategoryResponseDTO createCategory(CategoryRequestDto extCategory) {
        SyncLog syncLog = createSyncLog("CATEGORIES");
        Category category = new Category();
        category.setExternalId(extCategory.getId());
        category.setShow(extCategory.getShow());
        category.setSortOrder(extCategory.getOrder());

        Category parent = findById(extCategory.getParent());
        category.setParent(parent);

        if (extCategory.getName() != null) {
            extCategory.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    category.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    category.setNameEn(name.getText());
                }
            });
        }

        String baseName = category.getNameEn() != null ? category.getNameEn() : category.getNameBg();
        category.setSlug(generateSlug(baseName));

        categoryRepository.save(category);

        syncLog.setStatus("SUCCESS");
        syncLog.setRecordsProcessed(1L);
        syncLog.setRecordsCreated(1L);
        syncLog.setRecordsUpdated(0L);

        log.info("Category created successfully with id: {}", category.getId());
        return convertToResponseDTO(category);
    }

    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDto requestDTO) {
        log.info("Updating category with id: {}", id);

        Category existingCategory = findById(id);

        if (categoryRepository.existsBySlugAndIdNot(requestDTO.getSlug(), id)) {
            throw new DuplicateResourceException("Category with slug '" + requestDTO.getSlug() + "' already exists");
        }

        updateCategoryFromDTO(existingCategory, requestDTO);
        Category updatedCategory = categoryRepository.save(existingCategory);

        log.info("Category updated successfully with id: {}", id);
        return convertToResponseDTO(updatedCategory);
    }

    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setShow(false);
        categoryRepository.save(category);

        log.info("Category soft deleted successfully with id: {}", id);
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private Category findBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
    }

    private SyncLog createSyncLog(String syncType) {
        SyncLog syncLog = new SyncLog();
        syncLog.setSyncType(syncType);
        syncLog.setStatus("IN_PROGRESS");
        return syncLogRepository.save(syncLog);
    }

    private String generateSlug(String name) {
        return name == null ? null :
                name.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
    }

    private void updateCategoryFromDTO(Category category, CategoryRequestDto dto) {
        if (dto.getName() != null) {
            dto.getName().forEach(name -> {
                if ("bg".equals(name.getLanguageCode())) {
                    category.setNameBg(name.getText());
                } else if ("en".equals(name.getLanguageCode())) {
                    category.setNameEn(name.getText());
                }
            });
        }
        category.setSlug(dto.getSlug());
        category.setSortOrder(dto.getOrder());

        if (dto.getParent() != null) {
            Category parent = findById(dto.getParent());
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    private CategoryResponseDTO convertToResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .externalId(category.getExternalId())
                .nameEn(category.getNameEn())
                .nameBg(category.getNameBg())
                .slug(category.getSlug())
                .show(category.getShow())
                .sortOrder(category.getSortOrder())
                .parent(category.getParent() != null ? convertToSummaryDTO(category.getParent()) : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .isParentCategory(category.isParentCategory())
                .build();
    }

    private CategorySummaryDTO convertToSummaryDTO(Category category) {
        return CategorySummaryDTO.builder()
                .id(category.getId())
                .nameEn(category.getNameEn())
                .nameBg(category.getNameBg())
                .slug(category.getSlug())
                .show(category.getShow())
                .build();
    }
}

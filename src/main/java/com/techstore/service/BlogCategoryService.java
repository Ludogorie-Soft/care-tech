package com.techstore.service;

import com.techstore.dto.request.BlogCategoryRequestDto;
import com.techstore.dto.response.BlogCategoryResponseDto;
import com.techstore.entity.BlogCategory;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import com.techstore.repository.BlogCategoryRepository;
import com.techstore.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogCategoryService {

    private final BlogCategoryRepository blogCategoryRepository;

    @Transactional(readOnly = true)
    public List<BlogCategoryResponseDto> getAllCategories() {
        return blogCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(cat -> BlogCategoryResponseDto.from(cat, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BlogCategoryResponseDto> getCategoryTree() {
        // Load all categories in one query, assemble tree in memory
        List<BlogCategory> all = blogCategoryRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<Long, BlogCategoryResponseDto> dtoMap = new HashMap<>();
        List<BlogCategoryResponseDto> roots = new ArrayList<>();

        // First pass: create all DTOs
        for (BlogCategory cat : all) {
            BlogCategoryResponseDto dto = BlogCategoryResponseDto.from(cat, false);
            dtoMap.put(cat.getId(), dto);
        }

        // Second pass: link children to parents
        for (BlogCategory cat : all) {
            BlogCategoryResponseDto dto = dtoMap.get(cat.getId());
            if (cat.getParent() == null) {
                roots.add(dto);
            } else {
                BlogCategoryResponseDto parent = dtoMap.get(cat.getParent().getId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }

        return roots;
    }

    @Transactional(readOnly = true)
    public BlogCategoryResponseDto getBySlug(String slug) {
        BlogCategory cat = blogCategoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с slug '" + slug + "' не е намерена"));
        return BlogCategoryResponseDto.from(cat, true);
    }

    @Transactional
    public BlogCategoryResponseDto createCategory(BlogCategoryRequestDto request) {
        String slug = resolveSlug(request.getSlug(), request.getName(), null);

        BlogCategory cat = new BlogCategory();
        cat.setName(request.getName());
        cat.setSlug(slug);
        cat.setDescription(request.getDescription());
        cat.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            BlogCategory parent = blogCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Родителска категория с ID " + request.getParentId() + " не е намерена"));
            cat.setParent(parent);
        }

        try {
            BlogCategory saved = blogCategoryRepository.save(cat);
            log.info("Created blog category: id={}, slug={}", saved.getId(), saved.getSlug());
            return BlogCategoryResponseDto.from(saved, false);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Категория с този slug вече съществува");
        }
    }

    @Transactional
    public BlogCategoryResponseDto updateCategory(Long id, BlogCategoryRequestDto request) {
        BlogCategory cat = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + id + " не е намерена"));

        String slug = resolveSlug(request.getSlug(), request.getName(), id);
        cat.setName(request.getName());
        cat.setSlug(slug);
        cat.setDescription(request.getDescription());
        cat.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessLogicException("Категорията не може да бъде собствен родител");
            }
            validateNoCircularReference(id, request.getParentId());
            BlogCategory parent = blogCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Родителска категория с ID " + request.getParentId() + " не е намерена"));
            cat.setParent(parent);
        } else {
            cat.setParent(null);
        }

        try {
            BlogCategory saved = blogCategoryRepository.save(cat);
            log.info("Updated blog category: id={}, slug={}", saved.getId(), saved.getSlug());
            return BlogCategoryResponseDto.from(saved, false);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Категория с този slug вече съществува");
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        BlogCategory cat = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + id + " не е намерена"));
        blogCategoryRepository.delete(cat);
        log.info("Deleted blog category: id={}", id);
    }

    private void validateNoCircularReference(Long categoryId, Long newParentId) {
        Long current = newParentId;
        while (current != null) {
            if (current.equals(categoryId)) {
                throw new BusinessLogicException("Избраният родител би създал циклична зависимост в категориите");
            }
            BlogCategory parent = blogCategoryRepository.findById(current).orElse(null);
            current = (parent != null && parent.getParent() != null) ? parent.getParent().getId() : null;
        }
    }

    private String resolveSlug(String requestedSlug, String name, Long existingId) {
        boolean userProvided = requestedSlug != null && !requestedSlug.isBlank();
        String slug = userProvided ? requestedSlug.trim().toLowerCase() : generateSlug(name);

        boolean duplicate = (existingId == null)
                ? blogCategoryRepository.existsBySlug(slug)
                : blogCategoryRepository.existsBySlugAndIdNot(slug, existingId);

        if (duplicate) {
            throw new DuplicateResourceException("Slug '" + slug + "' вече се използва от друга категория");
        }
        return slug;
    }

    private String generateSlug(String name) {
        String slug = SlugUtils.generateSlug(name);
        return slug.isBlank() ? "category-" + System.currentTimeMillis() : slug;
    }
}

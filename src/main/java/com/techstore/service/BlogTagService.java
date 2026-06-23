package com.techstore.service;

import com.techstore.dto.request.BlogTagRequestDto;
import com.techstore.dto.response.BlogTagResponseDto;
import com.techstore.entity.BlogTag;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import com.techstore.repository.BlogTagRepository;
import com.techstore.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogTagService {

    private final BlogTagRepository blogTagRepository;

    @Transactional(readOnly = true)
    public List<BlogTagResponseDto> getAllTags() {
        List<BlogTag> tags = blogTagRepository.findAllByOrderByNameAsc();
        Map<Long, Long> countMap = new HashMap<>();
        blogTagRepository.countPostsPerTag()
                .forEach(row -> countMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));
        return tags.stream()
                .map(tag -> BlogTagResponseDto.from(tag, countMap.getOrDefault(tag.getId(), 0L)))
                .toList();
    }

    @Transactional
    public BlogTagResponseDto createTag(BlogTagRequestDto request) {
        String name = request.getName().trim();
        if (blogTagRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Таг с името '" + name + "' вече съществува");
        }
        String slug = resolveSlug(request.getSlug(), name, null);

        BlogTag tag = new BlogTag();
        tag.setName(name);
        tag.setSlug(slug);
        try {
            BlogTag saved = blogTagRepository.save(tag);
            log.info("Created blog tag: id={}, slug={}", saved.getId(), saved.getSlug());
            return BlogTagResponseDto.from(saved, 0L);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Таг с това име или slug вече съществува");
        }
    }

    @Transactional
    public BlogTagResponseDto updateTag(Long id, BlogTagRequestDto request) {
        BlogTag tag = blogTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Таг с ID " + id + " не е намерен"));

        String name = request.getName().trim();
        if (!name.equalsIgnoreCase(tag.getName()) && blogTagRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Таг с името '" + name + "' вече съществува");
        }
        String slug = resolveSlug(request.getSlug(), name, id);
        tag.setName(name);
        tag.setSlug(slug);
        try {
            BlogTag saved = blogTagRepository.save(tag);
            return BlogTagResponseDto.from(saved, blogTagRepository.countPostsByTagId(saved.getId()));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Таг с това ime или slug вече съществува");
        }
    }

    @Transactional
    public void deleteTag(Long id) {
        BlogTag tag = blogTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Таг с ID " + id + " не е намерен"));
        blogTagRepository.delete(tag);
        log.info("Deleted blog tag: id={}", id);
    }

    private String resolveSlug(String requestedSlug, String name, Long existingId) {
        boolean userProvided = requestedSlug != null && !requestedSlug.isBlank();
        String slug = userProvided ? requestedSlug.trim().toLowerCase() : generateSlug(name);

        boolean duplicate = (existingId == null)
                ? blogTagRepository.existsBySlug(slug)
                : blogTagRepository.existsBySlugAndIdNot(slug, existingId);

        if (duplicate) {
            throw new DuplicateResourceException("Slug '" + slug + "' вече се използва от друг таг");
        }
        return slug;
    }

    private String generateSlug(String name) {
        String slug = SlugUtils.generateSlug(name);
        return slug.isBlank() ? "tag-" + System.currentTimeMillis() : slug;
    }
}

package com.techstore.service;

import com.techstore.dto.request.BlogPostRequestDto;
import com.techstore.dto.response.BlogPostResponseDto;
import com.techstore.dto.response.BlogPostSummaryDto;
import com.techstore.entity.BlogCategory;
import com.techstore.entity.BlogPost;
import com.techstore.entity.BlogTag;
import com.techstore.enums.BlogPostStatus;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.repository.BlogCategoryRepository;
import com.techstore.repository.BlogPostRepository;
import com.techstore.repository.BlogTagRepository;
import com.techstore.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPostService {

    private static final Pattern INLINE_IMG_PATTERN =
            Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final Set<String> RESERVED_SLUGS = Set.of("featured", "categories", "tags");

    private final BlogPostRepository blogPostRepository;
    private final BlogCategoryRepository blogCategoryRepository;
    private final BlogTagRepository blogTagRepository;
    private final S3Service s3Service;

    // ── Public read ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryDto> getPublishedPosts(Pageable pageable) {
        return blogPostRepository
                .findByStatusOrderByPublishedAtDesc(BlogPostStatus.PUBLISHED, pageable)
                .map(BlogPostSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryDto> getPublishedByCategory(String categorySlug, Pageable pageable) {
        return blogPostRepository.findPublishedByCategorySlug(categorySlug, BlogPostStatus.PUBLISHED, pageable)
                .map(BlogPostSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryDto> getPublishedByTag(String tagSlug, Pageable pageable) {
        return blogPostRepository.findPublishedByTagSlug(tagSlug, BlogPostStatus.PUBLISHED, pageable)
                .map(BlogPostSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryDto> getFeaturedPosts(Pageable pageable) {
        return blogPostRepository
                .findByFeaturedTrueAndStatusOrderByPublishedAtDesc(BlogPostStatus.PUBLISHED, pageable)
                .map(BlogPostSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public BlogPostResponseDto getBySlug(String slug) {
        BlogPost post = blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Статия с slug '" + slug + "' не е намерена"));

        if (!post.isPublished()) {
            throw new ResourceNotFoundException("Статия с slug '" + slug + "' не е намерена");
        }

        return BlogPostResponseDto.from(post);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        blogPostRepository.incrementViewCount(id);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishScheduledPosts() {
        // FOR UPDATE SKIP LOCKED ensures only one app instance processes each post,
        // preventing duplicate publishes in multi-instance deployments.
        List<BlogPost> due = blogPostRepository.findScheduledDueWithLock(LocalDateTime.now());
        if (due.isEmpty()) return;
        for (BlogPost post : due) {
            post.setStatus(BlogPostStatus.PUBLISHED);
            blogPostRepository.save(post);
            log.info("Auto-published scheduled blog post: id={}, slug={}", post.getId(), post.getSlug());
        }
    }

    // ── Admin read ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryDto> getAllPosts(Pageable pageable, BlogPostStatus status, String search) {
        boolean hasSearch = search != null && !search.isBlank();
        Page<BlogPost> page;
        if (status != null && hasSearch) {
            page = blogPostRepository.findByStatusAndTitleContainingIgnoreCase(status, search.trim(), pageable);
        } else if (status != null) {
            page = blogPostRepository.findByStatus(status, pageable);
        } else if (hasSearch) {
            page = blogPostRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
        } else {
            page = blogPostRepository.findAll(pageable);
        }
        return page.map(BlogPostSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public BlogPostResponseDto getById(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Статия с ID " + id + " не е намерена"));
        return BlogPostResponseDto.from(post);
    }

    // ── Admin write ──────────────────────────────────────────────────────────

    @Transactional
    public BlogPostResponseDto createPost(BlogPostRequestDto request) {
        String slug = resolveSlug(request.getSlug(), request.getTitle(), null);
        BlogPostStatus status = parseStatus(request.getStatus());

        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle());
        post.setSlug(slug);
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setCoverImageUrl(request.getCoverImageUrl());
        post.setStatus(status);
        post.setFeatured(request.isFeatured());
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDescription(request.getMetaDescription());

        applyPublishedAt(post, null, status, request.getPublishedAt());
        applyCategory(post, request.getCategoryId());
        applyTags(post, request.getTagIds());

        try {
            BlogPost saved = blogPostRepository.save(post);
            log.info("Created blog post: id={}, slug={}, status={}", saved.getId(), saved.getSlug(), saved.getStatus());
            return BlogPostResponseDto.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Slug '" + slug + "' вече се използва от друга статия");
        }
    }

    @Transactional
    public BlogPostResponseDto updatePost(Long id, BlogPostRequestDto request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Статия с ID " + id + " не е намерена"));

        String slug = resolveSlug(request.getSlug(), request.getTitle(), id);
        BlogPostStatus previousStatus = post.getStatus();
        BlogPostStatus newStatus = parseStatus(request.getStatus());

        String oldCoverUrl = post.getCoverImageUrl();
        Set<String> oldInlineUrls = extractInlineImageUrls(post.getContent());

        post.setTitle(request.getTitle());
        post.setSlug(slug);
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setCoverImageUrl(request.getCoverImageUrl());
        post.setStatus(newStatus);
        post.setFeatured(request.isFeatured());
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDescription(request.getMetaDescription());

        applyPublishedAt(post, previousStatus, newStatus, request.getPublishedAt());
        applyCategory(post, request.getCategoryId());
        applyTags(post, request.getTagIds());

        try {
            BlogPost saved = blogPostRepository.save(post);
            log.info("Updated blog post: id={}, slug={}, status={}", saved.getId(), saved.getSlug(), saved.getStatus());

            // S3 cleanup
            String newCoverUrl = request.getCoverImageUrl();
            if (!Objects.equals(oldCoverUrl, newCoverUrl) && oldCoverUrl != null && !oldCoverUrl.isBlank()) {
                try { s3Service.deleteImage(oldCoverUrl); }
                catch (Exception e) { log.warn("Could not delete old cover from S3 for post {}: {}", id, e.getMessage()); }
            }
            Set<String> newInlineUrls = extractInlineImageUrls(request.getContent());
            oldInlineUrls.removeAll(newInlineUrls);
            deleteInlineImages(oldInlineUrls, saved.getId());

            return BlogPostResponseDto.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Slug '" + slug + "' вече се използва от друга статия");
        }
    }

    @Transactional
    public void deletePost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Статия с ID " + id + " не е намерена"));

        String coverUrl = post.getCoverImageUrl();
        String content = post.getContent();

        blogPostRepository.delete(post);
        log.info("Deleted blog post: id={}", id);

        if (coverUrl != null && !coverUrl.isBlank()) {
            try { s3Service.deleteImage(coverUrl); }
            catch (Exception e) { log.warn("Could not delete cover from S3 for post {}: {}", id, e.getMessage()); }
        }
        deleteInlineImages(content, id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BlogPostStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return BlogPostStatus.DRAFT;
        try {
            return BlogPostStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BlogPostStatus.DRAFT;
        }
    }

    private void applyPublishedAt(BlogPost post, BlogPostStatus previousStatus, BlogPostStatus newStatus, LocalDateTime requestedPublishedAt) {
        if (newStatus == BlogPostStatus.PUBLISHED) {
            if (previousStatus != BlogPostStatus.PUBLISHED) {
                // Първо публикуване, или ръчно от SCHEDULED (публ. датата може да е в бъдещето)
                if (post.getPublishedAt() == null || post.getPublishedAt().isAfter(LocalDateTime.now())) {
                    post.setPublishedAt(LocalDateTime.now());
                }
            }
        } else if (newStatus == BlogPostStatus.SCHEDULED) {
            if (requestedPublishedAt != null) {
                post.setPublishedAt(requestedPublishedAt);
            }
        } else {
            // DRAFT — clear publishedAt
            post.setPublishedAt(null);
        }
    }

    private void applyCategory(BlogPost post, Long categoryId) {
        if (categoryId != null) {
            BlogCategory cat = blogCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + categoryId + " не е намерена"));
            post.setCategory(cat);
        } else {
            post.setCategory(null);
        }
    }

    private void applyTags(BlogPost post, List<Long> tagIds) {
        post.getTags().clear();
        if (tagIds == null || tagIds.isEmpty()) return;
        List<BlogTag> found = blogTagRepository.findAllById(tagIds);
        if (found.size() != tagIds.size()) {
            Set<Long> foundIds = new HashSet<>();
            found.forEach(t -> foundIds.add(t.getId()));
            Long missing = tagIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Таг с ID " + missing + " не е намерен");
        }
        post.getTags().addAll(found);
    }

    private String resolveSlug(String requestedSlug, String title, Long existingId) {
        boolean userProvided = requestedSlug != null && !requestedSlug.isBlank();
        String slug = userProvided ? requestedSlug.trim().toLowerCase() : generateSlug(title);

        if (RESERVED_SLUGS.contains(slug)) {
            throw new DuplicateResourceException("Slug '" + slug + "' е запазен и не може да се използва");
        }

        boolean duplicate = (existingId == null)
                ? blogPostRepository.existsBySlug(slug)
                : blogPostRepository.existsBySlugAndIdNot(slug, existingId);

        if (duplicate) {
            String msg = userProvided
                    ? "Slug '" + slug + "' вече се използва от друга статия"
                    : "Вече съществува статия с подобно заглавие. Въведете slug ръчно.";
            throw new DuplicateResourceException(msg);
        }
        return slug;
    }

    private String generateSlug(String title) {
        String slug = SlugUtils.generateSlug(title);
        return slug.isBlank() ? "post-" + System.currentTimeMillis() : slug;
    }

    private Set<String> extractInlineImageUrls(String content) {
        Set<String> urls = new HashSet<>();
        if (content == null || content.isBlank()) return urls;
        Matcher matcher = INLINE_IMG_PATTERN.matcher(content);
        while (matcher.find()) urls.add(matcher.group(1));
        return urls;
    }

    private void deleteInlineImages(String content, Long excludeId) {
        deleteInlineImages(extractInlineImageUrls(content), excludeId);
    }

    private void deleteInlineImages(Set<String> urls, Long excludeId) {
        for (String url : urls) {
            long sharedCount = blogPostRepository.countOtherPostsReferencingUrl(url, excludeId);
            if (sharedCount > 0) {
                log.debug("Skipping S3 deletion — inline image referenced by {} other post(s): {}", sharedCount, url);
                continue;
            }
            try { s3Service.deleteImage(url); }
            catch (Exception e) { log.warn("Could not delete inline image from S3: {}", url); }
        }
    }
}

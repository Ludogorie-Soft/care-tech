package com.techstore.repository;

import java.time.LocalDateTime;

public interface SitemapEntry {
    Long getId();
    String getSlug();
    LocalDateTime getUpdatedAt();
}

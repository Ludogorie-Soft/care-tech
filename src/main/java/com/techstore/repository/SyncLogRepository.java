package com.techstore.repository;

import com.techstore.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}
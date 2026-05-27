package com.techstore.repository;

import com.techstore.entity.SyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    @Query("SELECT s FROM SyncLog s WHERE " +
           "(:syncType IS NULL OR s.syncType = :syncType) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<SyncLog> findByFilters(
            @Param("syncType") String syncType,
            @Param("status") String status,
            Pageable pageable);
}
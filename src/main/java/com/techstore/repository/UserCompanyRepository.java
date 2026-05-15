package com.techstore.repository;

import com.techstore.entity.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    List<UserCompany> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<UserCompany> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE UserCompany c SET c.isDefault = false WHERE c.user.id = :userId AND c.id <> :excludeId")
    void clearDefaultForUser(@Param("userId") Long userId, @Param("excludeId") Long excludeId);

    long countByUserId(Long userId);
}

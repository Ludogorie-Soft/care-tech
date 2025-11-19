package com.techstore.repository;

import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParameterOptionRepository extends JpaRepository<ParameterOption, Long> {

    Optional<ParameterOption> findByExternalIdAndParameterId(Long externalId, Long parameterId);

    List<ParameterOption> findByParameterIdOrderByOrderAsc(Long parameterId);

    Optional<ParameterOption> findByParameterAndNameBg(Parameter parameter, String nameBg);

    @Query("SELECT po FROM ParameterOption po JOIN po.parameter p WHERE po.externalId IN :externalIds AND p.category.id = :categoryId")
    List<ParameterOption> findByExternalIdInAndParameterCategoryId(@Param("externalIds") Collection<Long> externalIds, @Param("categoryId") Long categoryId);
}
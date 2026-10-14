package com.techstore.repository;

import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ParameterOptionRepository extends JpaRepository<ParameterOption, Long> {

    List<ParameterOption> findByParameterIdOrderByOrderAsc(Long parameterId);

    @Query("SELECT po FROM ParameterOption po " +
            "WHERE po.externalId IN :externalIds " +
            "AND po.parameter.id IN (" +
            "    SELECT p.id FROM Parameter p JOIN p.categories c WHERE c.id = :categoryId" +
            ")")
    List<ParameterOption> findByExternalIdInAndParameterCategoryId(
            @Param("externalIds") Set<Long> externalIds,
            @Param("categoryId") Long categoryId
    );

    @Query(value = "SELECT DISTINCT ON (name_bg, name_en) * " +
            "FROM parameter_options " +
            "WHERE parameter_id = :parameterId " +
            "ORDER BY name_bg, name_en, sort_order ASC",
            nativeQuery = true)
    List<ParameterOption> findUniqueOptionsByParameter(@Param("parameterId") Long parameterId);

    Optional<ParameterOption> findByParameterAndNameBg(Parameter parameter, String nameBg);
}
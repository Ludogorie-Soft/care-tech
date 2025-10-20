package com.techstore.repository;

import com.techstore.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {

    Optional<Manufacturer> findByExternalId(Long externalId);

    Optional<Manufacturer> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Manufacturer> findAllByOrderByNameAsc();

    @Query("SELECT m FROM Manufacturer m WHERE m.asbisId = :id")
    Optional<Manufacturer> findByAsbisId(@Param("id") String id);

    @Query("SELECT m FROM Manufacturer m WHERE m.asbisCode = :code")
    Optional<Manufacturer> findByAsbisCode(@Param("code") String code);

    @Query("SELECT m FROM Manufacturer m WHERE m.asbisCode IS NOT NULL")
    List<Manufacturer> findAllAsbisManufacturers();

    @Query("SELECT COUNT(m) FROM Manufacturer m WHERE m.asbisCode IS NOT NULL")
    Long countAsbisManufacturers();
}
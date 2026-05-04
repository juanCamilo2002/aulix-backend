package com.aulix.aulix_backend.tenant.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findByCustomDomain(String domain);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tenant t WHERE t.active = true")
    List<Tenant> findAllActive();
}


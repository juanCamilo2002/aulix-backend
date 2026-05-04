package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.model.Tenant;
import com.aulix.aulix_backend.tenant.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant createTenant(String slug, String name, String brandColor) {
        // 1. Validar y sanitizar slug
        String cleanSlug = slug.toLowerCase().replaceAll("[^a-z0-9_]", "");

        if (cleanSlug.isBlank()) {
            throw AulixException.badRequest("Slug inválido");
        }

        if (tenantRepository.existsBySlug(cleanSlug)) {
            throw AulixException.conflict("Ya existe un tenant con el slug: " + cleanSlug);
        }

        // 2. Crear schema en PostgreSQL
        createSchema(cleanSlug);

        // 3. Correr migraciones Flyway en ese schema
        migrateSchema(cleanSlug);

        // 4. Guardar en tabla pÃºblica de tenants
        Tenant tenant = Tenant.builder()
                .slug(cleanSlug)
                .name(name)
                .brandColor(brandColor != null ? brandColor : "#6366F1")
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant creado exitosamente: {}", cleanSlug);

        return saved;
    }

    private void createSchema(String slug) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + slug);
            log.info("Schema creado: {}", slug);

        } catch (SQLException e) {
            throw new RuntimeException("Error creando schema para tenant: " + slug, e);
        }
    }

    private void migrateSchema(String slug) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(slug)
                .locations("classpath:db/tenant-migration")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        log.info("Migraciones aplicadas en schema: {}", slug);
    }

    // Se ejecuta al arrancar la app â€” migra tenants existentes
    @EventListener(ApplicationReadyEvent.class)
    public void migrateAllTenants() {
        log.info("Verificando migraciones pendientes en todos los tenants...");
        tenantRepository.findAllActive()
                .forEach(t -> {
                    try {
                        migrateSchema(t.getSlug());
                    } catch (Exception e) {
                        log.error("Error migrando tenant {}: {}", t.getSlug(), e.getMessage());
                    }
                });
    }
}


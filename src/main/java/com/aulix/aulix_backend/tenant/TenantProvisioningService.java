package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.exception.AulixException;
import com.aulix.aulix_backend.tenant.model.Tenant;
import com.aulix.aulix_backend.tenant.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9_]{1,63}$");
    private static final String DUPLICATE_SCHEMA_SQL_STATE = "42P06";

    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    @Value("${app.platform.admin-tenant:acme}")
    private String platformAdminTenant;

    public Tenant createTenant(String slug, String name, String brandColor) {
        requirePlatformAdminTenant();
        String cleanSlug = normalizeSlug(slug);

        if (tenantRepository.existsBySlug(cleanSlug)) {
            throw AulixException.conflict("Ya existe un tenant con el slug: " + cleanSlug);
        }

        boolean schemaCreated = false;
        try {
            createSchema(cleanSlug);
            schemaCreated = true;

            migrateSchema(cleanSlug);

            Tenant tenant = Tenant.builder()
                    .slug(cleanSlug)
                    .name(name)
                    .brandColor(brandColor != null ? brandColor : "#6366F1")
                    .build();

            Tenant saved = tenantRepository.saveAndFlush(tenant);
            log.info("Tenant creado exitosamente: {}", cleanSlug);
            return saved;
        } catch (RuntimeException ex) {
            if (schemaCreated) {
                dropSchemaQuietly(cleanSlug);
            }
            throw ex;
        }
    }

    private void createSchema(String slug) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA " + quoteIdentifier(slug));
            log.info("Schema creado: {}", slug);

        } catch (SQLException e) {
            if (DUPLICATE_SCHEMA_SQL_STATE.equals(e.getSQLState())) {
                throw AulixException.conflict("Ya existe un schema con el slug: " + slug);
            }
            throw new RuntimeException("Error creando schema para tenant: " + slug, e);
        }
    }

    private void dropSchemaQuietly(String slug) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(slug) + " CASCADE");
            log.warn("Schema eliminado tras fallo de provisioning: {}", slug);
        } catch (SQLException e) {
            log.error("No se pudo eliminar schema huérfano {}: {}", slug, e.getMessage());
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
                        throw new IllegalStateException("No se pudo migrar tenant activo: " + t.getSlug(), e);
                    }
                });
    }

    private void requirePlatformAdminTenant() {
        String currentTenant = TenantContext.getTenant();
        if (!platformAdminTenant.equals(currentTenant)) {
            throw AulixException.forbidden("Solo el tenant plataforma puede crear tenants");
        }
    }

    private String normalizeSlug(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase();
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw AulixException.badRequest("Slug inválido");
        }
        return normalized;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}


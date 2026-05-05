package com.aulix.aulix_backend.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class SchemaResolvingDataSource extends DelegatingDataSource {

    public SchemaResolvingDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applySchema(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applySchema(conn);
        return conn;
    }

    private void applySchema(Connection conn) throws SQLException {
        String tenant = TenantContext.getTenant();

        String schema = (tenant == null || tenant.isBlank())
                ? "public"
                : tenant.replaceAll("[^a-z0-9_]", "");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + schema + ", public");
            log.debug("search_path seteado a: {}, public", schema);
        }
    }
}





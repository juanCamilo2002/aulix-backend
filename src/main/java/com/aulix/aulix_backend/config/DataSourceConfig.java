package com.aulix.aulix_backend.config;

import com.aulix.aulix_backend.tenant.SchemaResolvingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
        return properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariDataSource hikariDataSource) {
        // SchemaResolvingDataSource cambia el search_path según el tenant activo
        SchemaResolvingDataSource schemaResolving =
                new SchemaResolvingDataSource(hikariDataSource);

        // LazyConnectionDataSourceProxy evita obtener una conexión hasta
        // que realmente se necesite importante para que el TenantContext
        // ya está seteado cuando se pida la conexión
        return new LazyConnectionDataSourceProxy(schemaResolving);
    }
}


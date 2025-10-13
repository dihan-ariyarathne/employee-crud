package com.acme.employee.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(SchemaProperties schema, CorsProperties cors) {

    public AppProperties {
        if (schema == null) {
            schema = new SchemaProperties(200, 300);
        }
        if (cors == null) {
            cors = new CorsProperties(List.of("http://localhost:5173"));
        }
    }

    public record SchemaProperties(
            int sampleSize,
            int cacheTtlSeconds) {

        public SchemaProperties(@DefaultValue("200") int sampleSize,
                                @DefaultValue("300") int cacheTtlSeconds) {
            this.sampleSize = sampleSize;
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }

    public record CorsProperties(List<String> allowedOrigins) {

        public CorsProperties {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                allowedOrigins = List.of("http://localhost:5173");
            }
        }
    }
}


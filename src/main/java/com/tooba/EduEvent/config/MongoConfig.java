package com.tooba.EduEvent.config;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.concurrent.TimeUnit;

/**
 * Enables Spring Data MongoDB auditing so that fields annotated with
 * {@code @CreatedDate} (createdAt / issuedAt / registeredAt / etc.) are
 * populated automatically on insert — the MongoDB equivalent of the old
 * Hibernate {@code @CreationTimestamp}.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /**
     * PERF: keep a few warm connections open. Without this, after any idle period
     * the next request pays a full TLS handshake to Atlas (often 300-1000 ms).
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoPoolCustomizer() {
        return builder -> builder
                .applyToConnectionPoolSettings(pool -> pool
                        .minSize(2)
                        .maxSize(20)
                        .maxConnectionIdleTime(10, TimeUnit.MINUTES))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS));
    }
}

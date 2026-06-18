package com.tooba.EduEvent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables Spring Data MongoDB auditing so that fields annotated with
 * {@code @CreatedDate} (createdAt / issuedAt / registeredAt / etc.) are
 * populated automatically on insert — the MongoDB equivalent of the old
 * Hibernate {@code @CreationTimestamp}.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}

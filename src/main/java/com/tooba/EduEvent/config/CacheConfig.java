package com.tooba.EduEvent.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // This simple annotation turns on Spring's caching engine!
}
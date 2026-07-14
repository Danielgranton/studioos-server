package com.studioos.server.search.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
}

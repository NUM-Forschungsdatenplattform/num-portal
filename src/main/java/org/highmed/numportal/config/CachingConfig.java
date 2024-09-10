package org.highmed.numportal.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static org.highmed.numportal.service.UserService.TRANSLATION_CACHE;
import static org.highmed.numportal.service.UserService.USERS_CACHE;

@Configuration
@EnableCaching
public class CachingConfig {

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();

    Cache aqlParametersCache = new ConcurrentMapCache("aqlParameters", false);
    Cache usersCache = new ConcurrentMapCache(USERS_CACHE, false);
    Cache translationsCache = new ConcurrentMapCache(TRANSLATION_CACHE, false);

    cacheManager.setCaches(Arrays.asList(aqlParametersCache, usersCache, translationsCache));

    return cacheManager;
  }
}

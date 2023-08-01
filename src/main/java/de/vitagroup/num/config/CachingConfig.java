package de.vitagroup.num.config;

import java.util.Arrays;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static de.vitagroup.num.service.UserService.TRANSLATION_CACHE;

@Configuration
@EnableCaching
public class CachingConfig {

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();

    Cache aqlParametersCache = new ConcurrentMapCache("aqlParameters", false);
    Cache usersCache = new ConcurrentMapCache("users", false);
    Cache translationsCache = new ConcurrentMapCache(TRANSLATION_CACHE, false);

    cacheManager.setCaches(Arrays.asList(aqlParametersCache, usersCache, translationsCache));

    return cacheManager;
  }
}

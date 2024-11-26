package fi.vm.sade.lokalisointi.configuration;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimpleCacheCustomizer implements CacheManagerCustomizer<ConcurrentMapCacheManager> {
  @Override
  public void customize(final ConcurrentMapCacheManager cacheManager) {
    cacheManager.setCacheNames(List.of("find"));
  }
}

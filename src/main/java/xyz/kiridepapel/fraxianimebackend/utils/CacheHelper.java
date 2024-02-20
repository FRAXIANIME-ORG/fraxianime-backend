package xyz.kiridepapel.fraxianimebackend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;

@Component
@SuppressWarnings("null")
@Log
public class CacheHelper {
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  
  public static <T> T searchFromCache(CacheManager cacheManager, Class<T> type, String cacheName, String cacheKey) {
    Cache cache = cacheManager.getCache(cacheName);
    T chapterCache = cache != null ? cache.get(cacheKey, type) : null;
    return chapterCache != null ? chapterCache : null;
  }
  
  public static void deleteFromCache(CacheManager cacheManager, String cacheName, String cacheKey, boolean deleteAllCollection) {
    Cache cache = cacheManager.getCache(cacheName);

    if (cache != null) {
      if (deleteAllCollection) {
        cache.clear();
        log.info("El cache de la coleccion '" + cacheName + "' fue eliminado");
      } else if (cacheKey != null) {
        try {
          cache.evict(cacheKey);
          log.info("El cache '" + cacheKey + "' de la coleccion '" + cacheName + "' fue eliminado");
        } catch (Exception e) {
          log.info("El cache '" + cacheKey + "' de la coleccion '" + cacheName + "' no existe");
        }
      }
    }
  }
}

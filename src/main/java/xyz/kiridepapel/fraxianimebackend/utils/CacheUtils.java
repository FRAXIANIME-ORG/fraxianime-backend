package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.repositories.SpecialCaseDaoRepository;

@Component
@Log
public class CacheUtils {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_2}")
  private String provider2;
  // Inyeccion de dependencias
  @Autowired
  private SpecialCaseDaoRepository specialCaseRepository;
  
  @Cacheable(value = "specialCases", key = "#type")
  public List<SpecialCaseEntity> getSpecialCases(Character type) {
    List<SpecialCaseEntity> specialCases = specialCaseRepository.findAll();
    List<SpecialCaseEntity> specialCasesSolicited = new ArrayList<SpecialCaseEntity>();
    
    specialCases.stream().filter(specialCase -> specialCase.getType().equals(type))
        .forEach(specialCasesSolicited::add);

    return specialCasesSolicited;
  }

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

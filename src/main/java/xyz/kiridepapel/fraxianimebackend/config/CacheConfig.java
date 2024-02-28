package xyz.kiridepapel.fraxianimebackend.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {
  @Value("${REDIS_HOST}")
  private String redisHost;
  @Value("${REDIS_PASSWORD}")
  private String redisPassword;
  @Value("${REDIS_PORT}")
  private int redisPort;

  @Value("${HOME_CACHE_TIME}")
  private Integer homeCacheTime;
  @Value("${LAST_CHAPTERS_CACHE_TIME}")
  private Integer lastChaptersCacheTime;
  @Value("${DIRECTORY_OPTIONS_CACHE_TIME}")
  private Integer directoryOptionsCacheTime;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
    redisConfig.setHostName(redisHost);
    redisConfig.setPort(redisPort);
    redisConfig.setPassword(RedisPassword.of(this.redisPassword));
    return new LettuceConnectionFactory(redisConfig);
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    // Configuración por defecto para todos las cachés
    RedisCacheConfiguration homeCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(homeCacheTime));

    RedisCacheConfiguration lastChaptersCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(lastChaptersCacheTime));

    RedisCacheConfiguration directoryOptionsCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofDays(directoryOptionsCacheTime));
    
    return RedisCacheManager.builder(redisConnectionFactory)
      .cacheDefaults(homeCacheConfig)
      .withCacheConfiguration("chapter", lastChaptersCacheConfig)
      .withCacheConfiguration("directory", directoryOptionsCacheConfig)
      .build();
  }

}

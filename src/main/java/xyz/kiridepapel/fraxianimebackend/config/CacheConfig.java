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

  @Value("${DEFAULT_CACHE_TIME}")
  private Integer defaultCacheTime;
  @Value("${LAST_CHAPTERS_CACHE_TIME}")
  private Integer lastChaptersCacheTime;

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
    // Configuración por defecto para todas las cachés
    RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(defaultCacheTime));

    // Configuración especial para la caché para los últimos capítulos
    RedisCacheConfiguration lastChaptersCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(lastChaptersCacheTime));
      
    return RedisCacheManager.builder(redisConnectionFactory)
      .cacheDefaults(defaultCacheConfig)
      .withCacheConfiguration("chapter", lastChaptersCacheConfig)
      .build();
  }

}

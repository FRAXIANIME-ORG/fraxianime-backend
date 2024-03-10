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
  // Variables estaticas
  @Value("${REDIS_HOST}")
  private String redisHost;
  @Value("${REDIS_PASSWORD}")
  private String redisPassword;
  @Value("${REDIS_PORT}")
  private int redisPort;
  // Variables estaticas de Cache
  @Value("${HOME_CACHE_TIME}")
  private Integer homeCacheTime;
  @Value("${LAST_CHAPTERS_CACHE_TIME}")
  private Integer lastChaptersCacheTime;
  @Value("${DIRECTORY_CACHE_TIME}")
  private Integer directoryCacheTime;
  @Value("${SCHEDULE_CACHE_TIME}")
  private Integer scheduleCacheTime;
  @Value("${ACTUAL_YEAR_TOP_CACHE_TIME}")
  private Integer actualYearTopCacheTime;

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
    // Defaukt: Unlimited time
    RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig();
    // Home
    RedisCacheConfiguration homeCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(homeCacheTime));
    // Last chapters (Home)
    RedisCacheConfiguration lastChaptersCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofDays(lastChaptersCacheTime));
    // Directory
    RedisCacheConfiguration directoryCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofDays(directoryCacheTime));
    // Schedule
    RedisCacheConfiguration scheduleCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(scheduleCacheTime));
    // Top
    RedisCacheConfiguration actualYearTopCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofDays(actualYearTopCacheTime));
    
    return RedisCacheManager.builder(redisConnectionFactory)
      .cacheDefaults(defaultCacheConfig)
      .withCacheConfiguration("home", homeCacheConfig)
      .withCacheConfiguration("specialCases", homeCacheConfig)
      .withCacheConfiguration("chapter", lastChaptersCacheConfig)
      .withCacheConfiguration("directory", directoryCacheConfig)
      .withCacheConfiguration("schedule", scheduleCacheConfig)
      .withCacheConfiguration("actualYearTop", actualYearTopCacheConfig)
      .build();
  }

}

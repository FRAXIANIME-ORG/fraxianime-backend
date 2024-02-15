package xyz.kiridepapel.fraxianimebackend.utils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.ConnectionFailed;

@Component
@SuppressWarnings("null")
@Log
public class DataUtils {
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;

  // ? Connection
  public Document simpleConnect(String url, String errorMessage) {
    try {
      return Jsoup.connect(url).get();
    } catch (Exception x) {
      throw new ConnectionFailed(errorMessage);
    }
  }

  // ? Redis
  public <T> T searchFromCache(CacheManager cacheManager, Class<T> type, String cacheName, String cacheKey) {
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

  // ? Data
  public static String decodeBase64(String encodedString, boolean isIframe) {
    if (!isIframe) {
      return new String(Base64.getDecoder().decode(encodedString));
    } else {
      String decodedString = new String(Base64.getDecoder().decode(encodedString));

      Pattern pattern = Pattern.compile("<iframe[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(decodedString);

      if (matcher.find()) {
        return matcher.group(1);
      } else {
        return decodedString;
      }
    }
  }

  // ? Text
  public static String removeDiacritics(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    // Remover caracteres diacríticos
    return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  // ? Date
  public static LocalDateTime getLocalDateTimeNow(Boolean isProduction) {
    return isProduction ? LocalDateTime.now().minusHours(5) : LocalDateTime.now();
  }

  public static Date getDateNow(Boolean isProduction) {
    return isProduction ? new Date(System.currentTimeMillis() - 18000000) : new Date();
  }

  public static String parseDate(String date, DateTimeFormatter formatter, int daysToModify) {
    if (date == null || date.isEmpty()) {
      return null;
    }

    LocalDate currentDate = LocalDate.parse(date, formatter);
    LocalDate nextChapterDate = currentDate.plusDays(daysToModify);

    return nextChapterDate.format(formatter);
  }

  public static String firstUpper(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }

}

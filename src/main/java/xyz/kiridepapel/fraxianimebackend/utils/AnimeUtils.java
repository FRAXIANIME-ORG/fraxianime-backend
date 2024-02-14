package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.repository.SpecialCaseRepository;

@Service
@Log
@SuppressWarnings("null")
public class AnimeUtils {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private SpecialCaseRepository specialCaseRepository;

  private List<String> animesWithoutZeroCases = List.of(
    // Anime url: one-piece-0X -> one-piece-X
    "shigatsu-wa-kimi-no-uso",
    "one-piece",
    "kimetsu-no-yaiba",
    "one-punch-man",
    "horimiya",
    "chuunibyou-demo-koi-ga-shitai",
    "chuunibyou-demo-koi-ga-shitai-ren",
    "bakemonogatari",
    "maou-gakuin-no-futekigousha",
    "maou-gakuin-no-futekigousha-2nd-season",
    "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita"
  );

  private List<String> chapterScriptCases = List.of(
    // Chapter url: one-piece-04 -> one-piece-03-2
    "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin-04"
  );

  public String specialNameOrUrlCases(Map<String, String> mapListType, String original, Character type) {
    try {
      String mapped = "";

      if (original.contains("/")) {
        String url = original.split("/")[0].trim();
        mapped = mapListType.getOrDefault(url, null); // Si es una url
      } else {
        mapped = mapListType.getOrDefault(original, null); // Si es un nombre
      }

      return this.returnWithMsg(original, mapped, type);
    } catch (Exception e) {
      log.info("No se encontró un mapeo del original: " + original + " y el tipo: " + type + " en la base de datos. " + "Error: " + e.getMessage());
      return "Valor vacio";
    }
  }

  public String specialNameOrUrlCase(String original, Character type) {
    try {
      String mapped = "";

      if (original.contains("/")) {
        String url = original.split("/")[0].trim();
        mapped = this.specialCaseRepository.getMappedByOriginalAndType(url, type); // Si es una url
      } else {
        mapped = this.specialCaseRepository.getMappedByOriginalAndType(original, type); // Si es un nombre
      }

      return this.returnWithMsg(original, mapped, type);
    } catch (Exception e) {
      log.info("No se encontró un mapeo del original: " + original + " y el tipo: " + type + " en la base de datos. " + "Error: " + e.getMessage());
      return "Valor vacio";
    }
  }

  private String returnWithMsg(String original, String mapped, char type) {
    if (mapped != null){
      if (original.contains("/")) {
        String url = original.split("/")[0].trim();
        log.info("--------------------");
        log.info("| " + type + " | Original: " + original);
        log.info("| " + type + " | Final: " + original.replace(url, mapped));
        log.info("--------------------");
        return original.replace(url, mapped);
      } else {
        log.info("--------------------");
        log.info("| " + type + " | Original: " + original);
        log.info("| " + type + " | Final: " + original.replace(original, mapped));
        log.info("--------------------");
        return original.replace(original, mapped);
      }
    } else {
      return original;
    }
  }

  public String specialChapterCases(String urlChapter, String inputName, Integer chapter) {
    urlChapter = urlChapter + "-" + String.format("%02d", chapter); // chapter-05
    if (this.animesWithoutZeroCases.contains(inputName) && chapter < 10) {
      urlChapter = urlChapterWithoutZero(urlChapter);
    }
    if (this.chapterScriptCases.contains(urlChapter.replace(this.providerAnimeLifeUrl, ""))) {
      urlChapter = urlChapterWithScript(urlChapter);
    }
    return urlChapter;
  }

  // Recorre un mapa y cambia las claves por las que se le indiquen
  public static Map<String, Object> specialDataKeys(Map<String, Object> originalMap, Map<String, String> specialKeys) {
    Map<String, Object> newMap = new HashMap<>();
    
    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    return newMap;
  }

  // Convierte: one-piece-04 -> one-piece-4
  public static String urlChapterWithoutZero(String urlChapter) {
    String urlWithoutZero = urlChapter.replaceAll("-0(\\d+)$", "-$1");
    return urlWithoutZero;
  }

  // Convierte: anime-13 -> anime-12-2
  public static String urlChapterWithScript(String urlChapter) {
    int number = Integer.parseInt(urlChapter.replaceAll("^.*-(\\d+)$", "$1")) - 1;
    String urlWithScript = urlChapter.replaceAll("-(\\d+)$", "-" + String.format("%02d", number) + "-2");
    return urlWithScript;
  }

  // Convierte: chapter-55 -> chapter-54-5
  public static String urlChapterWithPoint(String urlChapter) {
    int number = Integer.parseInt(urlChapter.replaceAll("^.*-(\\d+)$", "$1")) - 1;
    String urlWithPoint = urlChapter.replaceAll("-(\\d+)$", "-" + String.format("%02d", number) + "-5");
    return urlWithPoint;
  }
  
  // Busca en caché
  public <T> T searchFromCache(CacheManager cacheManager, String cacheName, String cacheKey, Class<T> type) {
    Cache cache = cacheManager.getCache(cacheName);
    T chapterCache = cache != null ? cache.get(cacheKey, type) : null;
    return chapterCache != null ? chapterCache : null;
  }
  
}

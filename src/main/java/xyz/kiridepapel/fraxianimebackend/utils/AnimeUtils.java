package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;

@Service
@Log
@SuppressWarnings("null")
public class AnimeUtils {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
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
    "maou-gakuin-no-futekigousha-2nd-season"
  );
  private List<String> chapterScriptCases = List.of(
    // Chapter url: one-piece-04 -> one-piece-03-2
    "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin-04"
  );

  public String specialNameOrUrlCases(String name, char type) {
    Map<String, String> specialCases = new HashMap<>();

    // Map<String, String> map2 = Map.ofEntries(
    //   Map.entry("providerUrl", "solo-leveling"),
    //   Map.entry("myName", "Ore dake Level Up na Ken"),
    //   Map.entry("myUrl", "ore-dake-level-up-na-ken")
    // );

    // Map<String, Map<String, String>> map1 = Map.ofEntries(
    //   Map.entry("Solo Leveling", map2)
    // );

    // ? Home AnimeLife -> Home MIO
    if (type == 'h') {
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
      specialCases.put("solo-leveling", "ore-dake-level-up-na-ken"); // 1
      specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
      specialCases.put("chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"); // 2
      specialCases.put("maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"); // 4
      specialCases.put("maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"); // 5
    }
    // ? (Anime, Chapter): MIO -> AnimeLife (name)
    if (type == 'n') {
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
      specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
    }
    // ? Anime: MIO -> AnimeLife (url)
    if (type == 'a') {
      specialCases.put("ao-no-exorcist-shimane-illuminati-hen", "ao-no-exorcist-shin-series"); // 3
      specialCases.put("captain-tsubasa-season-2-junior-youth-hen", "captain-tsubasa-junior-youth-hen"); // 4
    }
    // ? Anime: MIO -> Jkanime (url)
    if (type == 'j') {
      specialCases.put("maou-gakuin-no-futekigousha", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e"); // 4
      specialCases.put("maou-gakuin-no-futekigousha-2nd-season", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii"); // 5
    }
    // ? Chapter: MIO -> AnimeLife (lista de animes)
    if (type == 'e') {
      specialCases.put("Ore dake Level Up na Ken", "Solo Leveling"); // 1
      specialCases.put("Chiyu Mahou no Machigatta Tsukaikata", "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin"); // 2
    }
    // ? Chapter: MIO -> AnimeLife (url)
    if (type == 's') {
      specialCases.put("ore-dake-level-up-na-ken", "solo-leveling"); // 1
      specialCases.put("chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"); // 2
    }

    for (Map.Entry<String, String> entry : specialCases.entrySet()) {
      if (
        name.equals(entry.getKey()) || // Names
        name.contains("/") && name.split("/")[0].trim().equals(entry.getKey()) // Urls
      ) {
        log.info("--------------------");
        log.info("- Reemplazado: " + entry.getKey());
        log.info("- Por: " + entry.getValue());
        log.info("--------------------");
        return name.replace(entry.getKey(), entry.getValue());
      }
    }

    return name;
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
  
  // Busca en caché
  public <T> T searchFromCache(CacheManager cacheManager, String cacheName, String cacheKey, Class<T> type) {
    Cache cache = cacheManager.getCache(cacheName);
    T chapterCache = cache != null ? cache.get(cacheKey, type) : null;
    return chapterCache != null ? chapterCache : null;
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
  
}

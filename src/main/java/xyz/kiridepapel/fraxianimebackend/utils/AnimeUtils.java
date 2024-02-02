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

  // Como se ve en MI Página
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

  public String specialNameOrUrlCases(String original, Character type) {
    try {
      String mapped = "";

      if (original.contains("/")) {
        mapped = this.specialCaseRepository.getMappedByOriginalAndType(original.split("/")[0].trim(), type); // Si es una url
      } else {
        mapped = this.specialCaseRepository.getMappedByOriginalAndType(original, type); // Si es un nombre
      }

      if (mapped != null){
        log.info("--------------------");
        log.info("| " + type + " | Original: " + original);
        log.info("| " + type + " | Reemplazado por: " + mapped);
        log.info("--------------------");
        return original.replace(original, mapped);
      } else {
        return original;
      }

    } catch (Exception e) {
      log.info("No se encontró un mapeo del original: " + original + " y el tipo: " + type + " en la base de datos. " + "Error: " + e.getMessage());
      return "Valor vacio";
    }

    






    // Map<String, String> specialCases = new HashMap<>();
    // String from = String.valueOf(type);

    // // 1. No se encontró en JKAnime
    // // 1. Sí se encontró en AnimeLife (anime y capítulos)

    // // 2. Se quiere cambiar todo sobre un ánime en AnimeLife para que se alinie con el de JKAnime

    // // ? Home AnimeLife -> Home MIO
    // if (type == 'h') {
    //   from = "Home";
    //   specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
    //   specialCases.put("solo-leveling", "ore-dake-level-up-na-ken"); // 1
    //   specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
    //   specialCases.put("chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"); // 2
    //   // Ya no se van a mostrar
    //   specialCases.put("maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"); // 4
    //   specialCases.put("maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"); // 5
    // }
    // // ? Anime: MIO -> Jkanime (url)
    // // 1: ("AnimeLife", "JKanime")
    // if (type == 's') {
    //   from = "Jkanime";
    //   specialCases.put("maou-gakuin-no-futekigousha", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e"); // 4
    //   specialCases.put("maou-gakuin-no-futekigousha-2nd-season", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii"); // 5
    // }
    // // ? Anime: MIO -> AnimeLife (url)
    // // 2: ("JKanime", "AnimeLife")
    // if (type == 'a') {
    //   from = "Anime";
    //   specialCases.put("ao-no-exorcist-shimane-illuminati-hen", "ao-no-exorcist-shin-series"); // 3
    //   specialCases.put("captain-tsubasa-season-2-junior-youth-hen", "captain-tsubasa-junior-youth-hen"); // 4
    //   specialCases.put("shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node");
    //   specialCases.put("mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2");
    //   specialCases.put("twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba");
    // }
    // // ? Chapter: MIO -> AnimeLife (url)
    // // 2: ("JKanime", "AnimeLife")
    // if (type == 'c') {
    //   from = "Chapter";
    //   specialCases.put("ore-dake-level-up-na-ken", "solo-leveling"); // Solo cambia en los capítulos
    //   specialCases.put("chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"); // 2
    //   specialCases.put("shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node");
    //   specialCases.put("mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2");
    //   specialCases.put("twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba");
    // }
    // // ? Anime, Chapter: AnimeLife -> MIO (name)
    // // 2: ("AnimeLife", "JKanime")
    // if (type == 'n') {
    //   from = "Name";
    //   specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
    //   specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
    //   specialCases.put("Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita"); // 3
    //   specialCases.put("Mushoku Tensei: Isekai Ittara Honki Dasu Part 2", "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season");
    //   specialCases.put("Boku no Kokoro no Yabai Yatsu: Twi-Yaba", "Twi-Yaba");
    // }
    // // ? Chapter: MIO -> AnimeLife (name: buscar en lista de animes)
    // // 2: ("JKanime", "AnimeLife")
    // if (type == 'l') {
    //   from = "List";
    //   specialCases.put("Ore dake Level Up na Ken", "Solo Leveling"); // 1
    //   specialCases.put("Chiyu Mahou no Machigatta Tsukaikata", "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin"); // 2
    //   specialCases.put("Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node"); // 3
    //   specialCases.put("Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season", "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2");
    //   specialCases.put("Twi-Yaba", "Boku no Kokoro no Yabai Yatsu: Twi-Yaba");
    // }
    // // specialCases.put("", "");

    // for (Map.Entry<String, String> entry : specialCases.entrySet()) {
    //   if (
    //     name.equals(entry.getKey()) || // Names
    //     name.contains("/") && name.split("/")[0].trim().equals(entry.getKey()) // Urls
    //   ) {
    //     log.info("--------------------");
    //     log.info("| " + from + " | Original: " + entry.getKey());
    //     log.info("| " + from + " | Reemplazado por: " + entry.getValue());
    //     log.info("--------------------");
    //     return name.replace(entry.getKey(), entry.getValue());
    //   }
    // }

    // return name;
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

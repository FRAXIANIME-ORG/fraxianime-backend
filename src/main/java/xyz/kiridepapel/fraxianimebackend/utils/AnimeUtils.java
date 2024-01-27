package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;

@Service
@Log
public class AnimeUtils {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;

  // Anime url: one-piece-0X -> one-piece-X
  private List<String> animesWithoutZeroCases = List.of(
    "shigatsu-wa-kimi-no-uso",
    "one-piece",
    "kimetsu-no-yaiba",
    "one-punch-man",
    "horimiya",
    "chuunibyou-demo-koi-ga-shitai",
    "chuunibyou-demo-koi-ga-shitai-ren",
    "bakemonogatari"
  );

  // Chapter url: one-piece-04 -> one-piece-03-2
  private List<String> chapterScriptCases = List.of(
    "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin-04"
  );

  public static Map<String, Object> specialDataKeys(Map<String, Object> originalMap, Map<String, String> specialKeys) {
    Map<String, Object> newMap = new HashMap<>();

    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    return newMap;
  }
  
  // ? Animes
  public String specialNameOrUrlCases(String urlName, char type) {
    Map<String, String> specialCases = new HashMap<>();

    // Map<String, String> map2 = Map.ofEntries(
    //   Map.entry("providerUrl", "solo-leveling"),
    //   Map.entry("myName", "Ore dake Level Up na Ken"),
    //   Map.entry("myUrl", "ore-dake-level-up-na-ken")
    // );

    // Map<String, Map<String, String>> map1 = Map.ofEntries(
    //   Map.entry("Solo Leveling", map2)
    // );

    if (type == 'h') { // Home: Proveedor página de inicio -> Mi página de inicio
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
      specialCases.put("solo-leveling", "ore-dake-level-up-na-ken"); // 1
      specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
      specialCases.put("chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"); // 2
    }
    if (type == 'n') { // Name: Proveedor anime, capítulo -> Mi anime, capítulo
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // 1
      specialCases.put("Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"); // 2
    }
    if (type == 's') { // Search: Usuario busca -> Proveedor busca
      specialCases.put("ore-dake-level-up-na-ken", "solo-leveling"); // 1
      specialCases.put("chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"); // 2
    }

    for (Map.Entry<String, String> entry : specialCases.entrySet()) {
      if (urlName.contains(entry.getKey())) {
        return urlName.replace(entry.getKey(), entry.getValue());
      }
    }

    return urlName;
  }

  public String specialChapterCases(String urlChapter, String inputName, Integer chapter) {
    urlChapter = urlChapter + "-" + String.format("%02d", chapter); // chapter-05

    // one-piece-04 -> one-piece-4
    if (chapter < 10) {
      if (this.animesWithoutZeroCases.contains(inputName)) {
        urlChapter = urlChapterWithoutZero(urlChapter);
      }
    }

    // anime-13 -> anime-12-2
    if (this.chapterScriptCases.contains(urlChapter.replace(this.providerAnimeLifeUrl, ""))) {
      urlChapter = urlChapterWithScript(urlChapter);
    }

    return urlChapter;
  }

  // one-piece-04 -> one-piece-4
  public static String urlChapterWithoutZero(String urlChapter) {
    String urlWithoutZero = urlChapter.replaceAll("-0(\\d+)$", "-$1");
    return urlWithoutZero;
  }

  public static String urlChapterWithScript(String urlChapter) { // 4, //3-2
    int number = Integer.parseInt(urlChapter.replaceAll("^.*-(\\d+)$", "$1")) - 1;
    String urlWithScript = urlChapter.replaceAll("-(\\d+)$", "-" + String.format("%02d", number) + "-2");
    return urlWithScript;
  }


}

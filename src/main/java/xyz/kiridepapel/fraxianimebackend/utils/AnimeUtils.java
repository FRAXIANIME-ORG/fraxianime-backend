package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimeUtils {
  // ? Generic
  public static boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public static boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }
  
  // ? Animes
  public static String specialNameOrUrlCases(String urlName, char type) {
    Map<String, String> specialCases = new HashMap<>();

    // h = Home to Anime
    if (type == 'h') {
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // Name
      specialCases.put("solo-leveling", "ore-dake-level-up-na-ken"); // Url
    }
    // c = Chapter to Provider Chapter // ore-dake-level-up-na-ken
    if (type == 'c') {
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // Name
      specialCases.put("ore-dake-level-up-na-ken", "solo-leveling"); // Url
    }

    for (Map.Entry<String, String> entry : specialCases.entrySet()) {
      if (urlName.contains(entry.getKey())) {
        return urlName.replace(entry.getKey(), entry.getValue());
      }
    }

    return urlName;
  }

}

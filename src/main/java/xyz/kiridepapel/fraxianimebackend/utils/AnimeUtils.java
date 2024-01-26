package xyz.kiridepapel.fraxianimebackend.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnimeUtils {
  // ? Generic
  public static boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public static boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  public static boolean isNotNullOrEmpty(Object obj) {
    return obj != null;
  }

  public static String parseDate(String date, int daysToModify) {
    if (date == null || date.isEmpty()) {
        return null;
    }

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    
    LocalDate currentDate = LocalDate.parse(date, inputFormatter);
    LocalDate nextChapterDate = currentDate.plusDays(daysToModify);

    return nextChapterDate.format(outputFormatter);
  }
  
  // ? Animes
  public static String specialNameOrUrlCases(String urlName, char type) {
    Map<String, String> specialCases = new HashMap<>();

    // h = Home to Anime
    if (type == 'h') {
      // Lo que llega del proveedor de la página de inicio, lo que será devuelto al usuario
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // Name
      specialCases.put("solo-leveling", "ore-dake-level-up-na-ken"); // Url
    }
    // c = Chapter to Provider Chapter 1: Lo que llega del proveedor al buscar un capítulo, lo que será devuelto al usuario
    if (type == 'c') {
      specialCases.put("Solo Leveling", "Ore dake Level Up na Ken"); // Name
    }
    // p = Provider Chapter to Chapter 2: Lo que llega del usuario al buscar un capítulo, lo que será enviado al proveedor para buscar el capítulo
    if (type == 'p') {
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

package xyz.kiridepapel.fraxianimebackend.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.java.Log;

@Log
public class AnimeUtils {
  // ? Generic
  public static boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public static boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  public static String parseDate(String date, int daysToModify) {
    if (date == null || date.isEmpty()) {
        return null;
    }

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", new Locale("es", "ES"));

    try {
      LocalDate currentDate = LocalDate.parse(date, inputFormatter);
      LocalDate nextChapterDate = currentDate.plusDays(daysToModify);

      return nextChapterDate.format(outputFormatter);
    } catch (DateTimeParseException e) {
      log.severe("Formato de fecha inválido: " + e.getMessage());
      return null;
    }
  }

  // public static String changeDays(String date, int daysToModify) {
  //   if (date == null) {
  //     return null;
  //   }

  //   try {
  //     DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
  //     DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", new Locale("es", "ES"));

  //     LocalDate currentDate = LocalDate.parse(date, inputFormatter);
  //     LocalDate modifiedDate = currentDate.plusDays(daysToModify);

  //     String formattedDate = modifiedDate.format(inputFormatter);
  //     return formattedDate;
  //   } catch (Exception e) {
  //     log.severe("Formato de fecha inválido: " + e.getMessage());
  //     return null;
  //   }
  // }
  
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

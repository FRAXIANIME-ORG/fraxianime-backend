package xyz.kiridepapel.fraxianimebackend.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;

@Log
@Component
public class DataUtils {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;

  // ? Connection
  public static Document simpleConnect(String urlHome, String errorMessage) {
    try {
      return Jsoup.connect(urlHome).get();
    } catch (Exception x) {
      throw new AnimeNotFound(errorMessage);
    }
  }
  
  public static Document tryConnectOrReturnNull(String urlAnimeInfo, Integer provider) {
    try {
      Document document = Jsoup.connect(urlAnimeInfo).get();

      if (provider == 1) {
        // Si existe .container, NO está en la página de error
        Element test = document.body().select(".container").first();
        return test != null ? document : null;
      }
      if (provider == 2) {
        // Si existe .postbody, NO está en la página de error
        Element test = document.body().select(".postbody").first();
        return test != null ? document : null;
      }

      // En cualquier otro caso, retornar null;
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public Document chapterSearchConnect(String urlChapter, Integer chapter, String errorMessage) {
    try {
      log.info("[] Last request url: " + urlChapter);
      return Jsoup.connect(urlChapter).get();
    } catch (Exception x) {
      if (chapter == 0) {
        // Si no se encontró 00, retornar error (no existe)
        throw new AnimeNotFound(errorMessage);
      } else {
        // Si no se encontró, se intenta sin el 0: one-piece-04 -> one-piece-4
        try { 
          String newUrl = AnimeUtils.urlChapterWithoutZero(urlChapter);
          log.info("[] Trying without zero: " + newUrl);
          return Jsoup.connect(newUrl).get();
        } catch (Exception xx) {
          // Si no se encontró, se intenta con guion y restando uno al capitulo: one-piece-04 -> one-piece-03-3
          try {
            String newUrl = AnimeUtils.urlChapterWithScript(urlChapter);
            log.info("[] Trying (<chapter> - 1)-2: " + newUrl);
            return Jsoup.connect(newUrl).get();
          } catch (Exception xxx) {
            throw new AnimeNotFound(errorMessage);
          }
        }
      }
    }
  }

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

  // ? Security
  public static boolean isSQLInjection(String str) {
    return str.matches(".*(--|[;+*^$|?{}\\[\\]()'\"\\']).*");
  }

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

  // ? Utils
  // La primera letra del formato de la fecha debe ser una letra, no un número.
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
    
}

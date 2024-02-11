package xyz.kiridepapel.fraxianimebackend.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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
  public Document simpleConnect(String urlHome, String errorMessage) {
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
      // Si ya busca 0 y no encuentra, el capitulo no existe
      if (chapter == 0) {
        throw new AnimeNotFound(errorMessage);
      } else {
        try {
          // No lo intenta si el capitulo es mayor a 9
          if (chapter >= 0 && chapter <= 9) {
            // Intenta: one-piece-04 -> one-piece-4
            String url1 = AnimeUtils.urlChapterWithoutZero(urlChapter);
            log.info("[] Trying without zero (-0X): " + url1);
            try {
              log.info("[] Founded!");
              return Jsoup.connect(url1).get();
            } catch (Exception e) {
              throw new AnimeNotFound(errorMessage);
            }
          } else {
            throw new Exception();
          }
        } catch (Exception xx) {
          // Intenta: one-piece-15 -> one-piece-14-2
          String url2 = AnimeUtils.urlChapterWithScript(urlChapter);
          log.info("[] Trying with script (-2): " + url2);
          try {
            log.info("[] Founded!");
            return Jsoup.connect(url2).get();
          } catch (Exception xxx) {
            // Intenta: one-piece-15 -> one-piece-14-5
            String url3 = AnimeUtils.urlChapterWithPoint(urlChapter);
            log.info("[] Trying with point (-5): " + url3);
            try {
              log.info("[] Founded!");
              return Jsoup.connect(url3).get();
            } catch (Exception e) {
              throw new AnimeNotFound(errorMessage);
            }
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

  // ? Utils
  public static String parseDate(String date, String pattern, int daysToModify) {
    if (date == null || date.isEmpty()) {
      return null;
    }

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(pattern, new Locale("es", "ES"));
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(pattern, new Locale("es", "ES"));

    LocalDate currentDate = LocalDate.parse(date, inputFormatter);
    LocalDate nextChapterDate = currentDate.plusDays(daysToModify);

    return nextChapterDate.format(outputFormatter);
  }
    
}

package xyz.kiridepapel.fraxianimebackend.utils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.ConnectionFailed;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.NextTrySearch;

@Log
@Component
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
  
  public static Document tryConnectOrReturnNull(String urlAnimeInfo, Integer provider) {
    try {
      Document document = Jsoup.connect(urlAnimeInfo).get();

      // JkAnime
      if (provider == 1) {
        // Si existe .container, NO está en la página de error
        Element test = document.body().select(".container").first();
        return test != null ? document : null;
      }
      // AnimeLife
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
      Document doc = tryConnectOrReturnNull(urlChapter, 2);
      if (doc != null) {
        log.info("[] Founded!");
        return doc;
      } else {
        throw new NextTrySearch();
      }
    } catch (NextTrySearch x) {
      // Si ya busca 0 y no encuentra, el capitulo no existe
      if (chapter == 0) {
        throw new ChapterNotFound(errorMessage);
      } else {
        try {
          // No lo intenta si el capitulo es mayor a 9
          if (chapter >= 0 && chapter <= 9) {
            // Intenta: one-piece-04 -> one-piece-4
            String url1 = AnimeUtils.urlChapterWithoutZero(urlChapter);
            log.info("[] Trying without zero (-0X): " + url1);
            Document doc = tryConnectOrReturnNull(url1, 2);
            if (doc != null) {
              log.info("[] Founded!");
              return doc;
            }
          }
          throw new NextTrySearch();
        } catch (NextTrySearch xx) {
          try {
            // Intenta: one-piece-15 -> one-piece-14-2
            String url2 = AnimeUtils.urlChapterWithScript(urlChapter);
            log.info("[] Trying with script (-2): " + url2);
            Document doc = tryConnectOrReturnNull(url2, 2);
            if (doc != null) {
              log.info("[] Founded!");
              return doc;
            } else {
              throw new NextTrySearch();
            }
          } catch (NextTrySearch xxx) {
            try {
              // Intenta: one-piece-15 -> one-piece-14-5
              String url3 = AnimeUtils.urlChapterWithPoint(urlChapter);
              log.info("[] Trying with point (-5): " + url3);
              Document doc = tryConnectOrReturnNull(url3, 2);
              if (doc != null) {
                log.info("[] Founded!");
                return doc;
              } else {
                throw new NextTrySearch();
              }
            } catch (NextTrySearch xxxx) {
              throw new ChapterNotFound(errorMessage);
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
    
  public static String removeDiacritics(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    // Remover caracteres diacríticos
    return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  public static LocalDateTime getLocalDateTimeNow(Boolean isProduction) {
    return isProduction ? LocalDateTime.now().minusHours(5) : LocalDateTime.now();
  }

  public static Date getDateNow(Boolean isProduction) {
    return isProduction ? new Date(System.currentTimeMillis() - 18000000) : new Date();
  }

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

package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;

public class DataUtils {

  // ? Connection
  public static Document connect(String urlAnimeInfo, String errorMessage, boolean tryWithoutZero) {
    try {
      return Jsoup.connect(urlAnimeInfo).get();
    } catch (Exception e) {
      if (tryWithoutZero) {
        String urlWithoutZero = urlAnimeInfo.replaceAll("-0(\\d+)$", "-$1");
        try { 
          return Jsoup.connect(urlWithoutZero).get();
        } catch (Exception ex) {
          throw new AnimeNotFound(errorMessage);
        }
      } else {
        throw new AnimeNotFound(errorMessage);
      }
    }
  }

  public static Document tryConnectOrReturnNull(String urlAnimeInfo, Integer provider) {
    try {
      Document document = Jsoup.connect(urlAnimeInfo).get();

      if (provider == 1) {
        Element test = document.body().select(".container").first();
        // Si existe .container, NO está en la página de error
        if (test != null) {
          return document;
        } else {
          return null;
        }
      }
      if (provider == 2) {
        Element test = document.body().select(".postbody").first();
        // Si existe .postbody, NO está en la página de error
        if (test != null) {
          return document;
        } else {
          return null;
        }
      }
      return null;
    } catch (Exception e) {
      return null;
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

  public static Map<String, Object> specialDataKeys(Map<String, Object> originalMap, Map<String, String> specialKeys) {
    Map<String, Object> newMap = new HashMap<>();

    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    return newMap;
  }
    
}

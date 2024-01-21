package xyz.kiridepapel.fraxianimebackend.service;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;

@Log
public class ZMethods {
  public static boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public static boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
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

  public static Document connectAnimeInfo(Document docAnimeInfo, String urlAnimeInfo, String errorMessage) {
    try {
      return docAnimeInfo = Jsoup.connect(urlAnimeInfo).get();
      // Si one-piece/1076 no existe
    } catch (Exception e) {
      try {
        // Se intenta con one-piece/1075-2
        int animeCap = Integer.parseInt(urlAnimeInfo.split("-episodio-")[1]);
        String pastCapUrl = String.valueOf(animeCap - 1) + "-2";
        urlAnimeInfo = urlAnimeInfo.replace("-episodio-" + animeCap, "-episodio-" + pastCapUrl);
        return docAnimeInfo = Jsoup.connect(urlAnimeInfo).get();
      } catch (Exception ex) {
        // Si one-piece/1075-2 no existe, retorna error
        log.warning(errorMessage + ": " + ex.getMessage());
        throw new AnimeNotFound(errorMessage);
      }
    }
  }
}

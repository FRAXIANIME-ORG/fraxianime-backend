package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;

public class DataUtils {

  // ? Connection
  public static Document connectAnimeInfo(String urlAnimeInfo, String errorMessage) {
    try {
      return Jsoup.connect(urlAnimeInfo).get();
    } catch (Exception e) {
      String urlWithoutZero = urlAnimeInfo.replaceAll("-0(\\d+)$", "-$1");
      try { 
        return Jsoup.connect(urlWithoutZero).get();
      } catch (Exception ex) {
        throw new AnimeNotFound(errorMessage);
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
    
}

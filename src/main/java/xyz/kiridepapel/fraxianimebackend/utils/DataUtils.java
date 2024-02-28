package xyz.kiridepapel.fraxianimebackend.utils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.ArgumentRequiredException;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.ConnectionFailed;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.ProtectedResource;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.SQLInjectionException;

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

  // ? Data
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

  public static String getClientIp() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    String ipAddress = request.getHeader("X-Forwarded-For");
    
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getHeader("Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getRemoteAddr();
    }
    if (ipAddress != null && ipAddress.contains(",")) {
      ipAddress = ipAddress.split(",")[0].trim();
    }

    return ipAddress;
  }

  // ? Text
  // Obtiene el nombre del anime de la URL (one-piece-1090 -> [one-piece])
  public static String getNameFromUrl(String baseUrl, String url) {
    String newUrl = url.replace(baseUrl, "");
    return newUrl.replaceAll("-\\d+/?$", "");
  }

  // Devuelve la última parte de la URL (part-2-05 -> [part])
  public static String getLastPartOfUrl(String url, String chapterPart) {
    url = url.replace("/", "").replace(chapterPart, "");
    String[] urlParts = url.split("-");
    return urlParts[urlParts.length - 1];
  }

  // Elimina las tildes
  public static String removeDiacritics(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }
  
  // Convierte la primera letra a mayúscula
  public static String firstUpper(String text) {
    return text.substring(0, 1).toUpperCase() + text.substring(1);
  }

  // Convierte el nombre de una variable a un nombre normal: "specialCase" -> "Special Case"
  public static String formatToNormalName(String name) {
    if (name.toLowerCase().equals("id")) return "ID";
    String formattedName = name.replaceAll("(\\B[A-Z])", " $1");
    return firstUpper(formattedName);
  }

  // ? Date
  public static LocalDateTime getLocalDateTimeNow(Boolean isProduction) {
    return isProduction ? LocalDateTime.now().minusHours(5) : LocalDateTime.now();
  }

  public static Date getDateNow(Boolean isProduction) {
    return isProduction ? new Date(System.currentTimeMillis() - 18000000) : new Date();
  }

  public static String parseDate(String date, DateTimeFormatter formatterIn, DateTimeFormatter formatterOut, int daysToModify) {
    if (date == null || date.isEmpty()) {
      return null;
    }

    LocalDate currentDate = LocalDate.parse(date, formatterIn);
    LocalDate nextChapterDate = currentDate.plusDays(daysToModify);

    return nextChapterDate.format(formatterOut);
  }

  // ? Validations
  public static void verifyAllowedOrigin(List<String> allowedOrigins, String origin) {
    if (origin == null || !allowedOrigins.contains(origin)) {
      throw new ProtectedResource("Acceso denegado TK-001");
    }
  }

  public static void isValidStr(String str, String errorMsg) {
    if (str == null || str.isEmpty()) {
      throw new ArgumentRequiredException(errorMsg);
    }
  }

  public static boolean isNotNullOrEmpty(Object obj) {
    return obj != null;
  }
  
  public static boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public static void verifySQLInjection(String str) {
    if (str.matches(".*(--|[;+*^$|?{}\\[\\]()'\"\\']).*") || str.contains("SELECT")) {
      throw new SQLInjectionException("Esas cosas son del diablo.");
    }
  }
}

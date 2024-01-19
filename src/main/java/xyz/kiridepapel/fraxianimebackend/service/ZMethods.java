package xyz.kiridepapel.fraxianimebackend.service;

import java.util.List;

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

  public static Document connectAnimeInfo(Document docAnimeInfo, String urlAnimeInfo, String errorMessage) {
    try {
      return docAnimeInfo = Jsoup.connect(urlAnimeInfo).get();
    } catch (Exception e) {
      log.warning(errorMessage + ": " + e.getMessage());
      throw new AnimeNotFound(errorMessage);
    }
  }
}

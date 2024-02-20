package xyz.kiridepapel.fraxianimebackend.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.NextTrySearch;
import xyz.kiridepapel.fraxianimebackend.service.ScheduleService;

@Service
@Log
public class AnimeUtils {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  // Inyección de dependencias
  @Autowired
  private ScheduleService scheduleService;
  // Variables
  private List<String> animesWithoutZeroCases = List.of(
    // Anime url: one-piece-0X -> one-piece-X
    "shigatsu-wa-kimi-no-uso",
    "one-piece",
    "kimetsu-no-yaiba",
    "one-punch-man",
    "horimiya",
    "chuunibyou-demo-koi-ga-shitai",
    "chuunibyou-demo-koi-ga-shitai-ren",
    "bakemonogatari",
    "maou-gakuin-no-futekigousha",
    "maou-gakuin-no-futekigousha-2nd-season",
    "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita",
    "karakai-jouzu-no-takagi-san"
  );
  private List<String> chapterScriptCases = List.of(
    // Chapter url: one-piece-04 -> one-piece-03-2
    "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin-04"
  );
  
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

  public Document chapterSearchConnect(String urlChapter, String chapter, String errorMessage) {
    Integer chapterPart = null;

    if (chapter.contains("-")) {
      chapterPart = Integer.parseInt(chapter.split("-")[0]);
    } else {
      chapterPart = Integer.parseInt(chapter);
    }

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
      if (chapterPart == 0) {
        throw new ChapterNotFound(errorMessage);
      } else {
        try {
          // No lo intenta si el capitulo es mayor a 9
          if (chapterPart >= 0 && chapterPart <= 9) {
            // Intenta: one-piece-04 -> one-piece-4
            String url1 = this.urlChapterWithoutZero(urlChapter);
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
            String url2 = this.urlChapterWithScript(urlChapter);
            log.info("[] Trying with script (-2): " + url2);
            Document doc = tryConnectOrReturnNull(url2, 2);
            if (doc != null) {
              log.info("[] Founded!");
              return doc;
            } else {
              throw new NextTrySearch();
            }
          } catch (NextTrySearch xxx) {
            throw new ChapterNotFound(errorMessage);
            // try {
            //   // Intenta: one-piece-15 -> one-piece-14-5
            //   String url3 = this.urlChapterWithPoint(urlChapter);
            //   log.info("[] Trying with point (-5): " + url3);
            //   Document doc = tryConnectOrReturnNull(url3, 2);
            //   if (doc != null) {
            //     log.info("[] Founded!");
            //     return doc;
            //   } else {
            //     throw new NextTrySearch();
            //   }
            // } catch (NextTrySearch xxxx) {
            //   throw new ChapterNotFound(errorMessage);
            // }
          }
        }
      }
    }
  }

  // Convierte: one-piece-04 -> one-piece-4
  public String urlChapterWithoutZero(String urlChapter) {
    String urlWithoutZero = urlChapter.replaceAll("-0(\\d+)$", "-$1");
    return urlWithoutZero;
  }

  // Convierte: anime-13 -> anime-12-2
  public String urlChapterWithScript(String urlChapter) {
    int number = Integer.parseInt(urlChapter.replaceAll("^.*-(\\d+)$", "$1")) - 1;
    String urlWithScript = urlChapter.replaceAll("-(\\d+)$", "-" + String.format("%02d", number) + "-2");
    return urlWithScript;
  }

  // // Convierte: chapter-55 -> chapter-54-5
  // public String urlChapterWithPoint(String urlChapter) {
  //   int number = Integer.parseInt(urlChapter.replaceAll("^.*-(\\d+)$", "$1")) - 1;
  //   String urlWithPoint = urlChapter.replaceAll("-(\\d+)$", "-" + String.format("%02d", number) + "-5");
  //   return urlWithPoint;
  // }
  
  // Mapea los casos especiales de nombres o urls (busca en cache en base al type)
  // Si se manda un mapListType, usa ese mapa en lugar de buscar en caché (type es opcional)
  public String specialNameOrUrlCases(Map<String, String> map, String original, char type, String fromMethod) {
    try {
      String mapped = "";
      Map<String, String> useMapListType = new HashMap<>();

      if (map != null) {
        useMapListType = Map.copyOf(map);
      } else {
        // Busca en cache en base al type
        List<SpecialCaseEntity> specialCases = this.scheduleService.getSpecialCases(type);
        for (SpecialCaseEntity specialCase : specialCases) {
          useMapListType.put(specialCase.getOriginal(), specialCase.getMapped());
        }
      }

      if (original.contains("/")) {
        // Si es una url
        String url = original.split("/")[0].trim();
        mapped = useMapListType.getOrDefault(url, null);
      } else {
        // Si es un nombre
        mapped = useMapListType.getOrDefault(original, null);
      }

      return this.returnWithMsg(original, mapped, type, fromMethod);
    } catch (Exception e) {
      log.severe("Ocurrió un error al intentar mapear '" + original + "': " + e.getMessage());
      return "Valor vacio";
    }
  }

  private String returnWithMsg(String original, String mapped, char type, String fromMethod) {
    if (mapped != null){
      if (original.contains("/")) {
        String url = original.split("/")[0].trim();
        // | h | animesProgramming() |  'Solo Leveling' -> 'Ore dake Level Up na Ken'
        log.info("--------------------");
        log.info("[ " + type + " | " + fromMethod + " ] Founded: '" + original + "'");
        log.info("[ " + type + " | " + fromMethod + " ] Changed: '" + original.replace(url, mapped) + "'");
        log.info("--------------------");
        return original.replace(url, mapped);
      } else {
        log.info("--------------------");
        log.info("[ " + type + " | " + fromMethod + " ] Founded:  '" + original + "'");
        log.info("[ " + type + " | " + fromMethod + " ] Changed: '" + original.replace(original, mapped) + "'");
        log.info("--------------------");
        return original.replace(original, mapped);
      }
    } else {
      return original;
    }
  }

  public String specialChapterCases(String urlChapter, String inputName, String chapter) {
    // Le da formato al capitulo si es necesario
    String chapterFormatted;
    if (!chapter.contains("-")) {
      chapterFormatted = String.format("%02d", Integer.parseInt(chapter));
    } else {
      chapterFormatted = chapter;
    }

    urlChapter = urlChapter + "-" + chapterFormatted; // chapter-05

    if (this.animesWithoutZeroCases.contains(inputName)) {
      urlChapter = urlChapterWithoutZero(urlChapter);
    }

    if (this.chapterScriptCases.contains(urlChapter.replace(this.providerAnimeLifeUrl, "")) && urlChapter.contains("-2")) {
      urlChapter = urlChapterWithScript(urlChapter);
    }

    return urlChapter;
  }

  // Recorre un mapa y cambia las claves por las que se le indiquen
  public static Map<String, Object> specialDataKeys(Map<String, Object> originalMap, Map<String, String> specialKeys) {
    Map<String, Object> newMap = new HashMap<>();
    
    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    return newMap;
  }
  
}

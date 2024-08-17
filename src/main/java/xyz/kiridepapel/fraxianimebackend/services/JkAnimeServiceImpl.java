package xyz.kiridepapel.fraxianimebackend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.AnimeHistoryDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.interfaces.IJkAnimeService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
public class JkAnimeServiceImpl implements IJkAnimeService {
  // Variables estaticas
  @Value("${PROVIDER_1}")
  private String provider1;
  // Variables
  private Map<String, String> tradMap;
  private Map<String, String> specialAltTitles;
  private Map<String, String> specialHistory;
  // Inyeccion de dependencias
  private final CacheUtils cacheUtils;
  private final AnimeUtils animeUtils;

  // Constructor
  public JkAnimeServiceImpl(CacheUtils cacheUtils, AnimeUtils animeUtils) {
    this.cacheUtils = cacheUtils;
    this.animeUtils = animeUtils;
  }

  // Inicialización
  @PostConstruct
  private void init() {
    this.tradMap = Map.ofEntries(
      Map.entry("Ene", "enero"),
      Map.entry("Feb", "febrero"),
      Map.entry("Mar", "marzo"),
      Map.entry("Abr", "abril"),
      Map.entry("May", "mayo"),
      Map.entry("Jun", "junio"),
      Map.entry("Jul", "julio"),
      Map.entry("Ago", "agosto"),
      Map.entry("Sep", "septiembre"),
      Map.entry("Oct", "octubre"),
      Map.entry("Nov", "noviembre"),
      Map.entry("Dic", "diciembre")
    );
    this.specialAltTitles = Map.ofEntries(
      Map.entry("Sinónimos", "synonyms"),
      Map.entry("Sinonimos", "synonyms"),
      Map.entry("sinonimos", "synonyms"),
      Map.entry("Inglés", "english"),
      Map.entry("Ingles", "english"),
      Map.entry("ingles", "english"),
      Map.entry("Japonés", "japanese"),
      Map.entry("Japones", "japanese"),
      Map.entry("japones", "japanese"),
      Map.entry("Coreano", "corean"),
      Map.entry("coreano", "corean")
    );
    this.specialHistory = Map.ofEntries(
      Map.entry("Precuela", "prequel"),
      Map.entry("Predecesor", "prequel"),
      Map.entry("Secuela", "sequel"),
      Map.entry("Version alternativa", "alternativeVersion"),
      Map.entry("Version completa", "completeVersion"),
      Map.entry("Adicional", "additional"),
      Map.entry("Resumen", "summary"),
      Map.entry("Personaje incluido", "includedCharacters"),
      Map.entry("Otro", "other"),
      Map.entry("Derivado", "derived")
    );
  }

  public AnimeInfoDTO getAnimeInfo(AnimeInfoDTO animeInfo, Document docJkanime, String search, boolean isMinDateInAnimeLf) {
    Element mainJkanime = docJkanime.body().select(".contenido").first();
    Elements keys = docJkanime.select(".anime__details__text .anime__details__widget .aninfo ul li");

    // Obtener la información del anime de jkanime
    String alternativeName = mainJkanime.select(".anime__details__title span").first().text().trim();
    String jkanimeImgUrl = mainJkanime.select(".anime__details__pic").first().attr("data-setbg").trim();
    String synopsis = mainJkanime.select(".sinopsis").text().trim();
    Integer likes = Integer.parseInt(mainJkanime.select(".anime__details__content .vot").first().text().trim());
    Object studios = this.getSpecificKey(keys, "Studios", true);
    String emited = String.valueOf(this.getSpecificKey(keys, "Emitido", false));
    String duration = String.valueOf(this.getSpecificKey(keys, "Duracion", false)).replace("por episodio", "").trim();
    String quality = String.valueOf(this.getSpecificKey(keys, "Calidad", false));
    Map<String, Object> alternativeTitles = this.getAlternativeTitlesJk(docJkanime);
    Map<String, Object> history = this.getHistoryJK(docJkanime);
    String trailer = mainJkanime.select(".animeTrailer").attr("data-yt");

    // Asignar la nueva información si es válida
    if (this.isValidData(alternativeName)) {
      animeInfo.setAlternativeName(alternativeName.replace("&#039;", "'"));
    }
    if (this.isValidData(jkanimeImgUrl)) {
      animeInfo.setImgUrl(jkanimeImgUrl);
    }
    if (this.isValidData(synopsis)) {
      animeInfo.setSynopsis(synopsis.replace("&#039;", "'"));
    }
    if (this.isValidData(likes)) {
      animeInfo.setLikes(likes);
    }
    if (studios != null && !studios.toString().trim().isEmpty()) {
      Elements stds = (Elements) studios;
      if (animeInfo.getData().get("Estudio") == null && this.isValidData(studios)) {
        List<LinkDTO> finalStudios = new ArrayList<>();
        for (Element studio : stds) {
          finalStudios.add(LinkDTO.builder()
            .name(studio.text().trim())
            .url("studio/" + studio.text().trim().toLowerCase().replaceAll(" ", "-"))
            .build());
        }
        animeInfo.getData().put("Estudio", finalStudios);
      }
    }
    if (this.isValidData(emited)) {
      String newEmited = this.defaultDateName(emited);
      animeInfo.getData().put("Publicado el", DataUtils.firstUpper(newEmited));
      
      if (isMinDateInAnimeLf == true) {
        animeInfo.setLastChapterDate(newEmited);
      }

      // Si hay un rango de fechas
      if (emited.contains(" a ")) {
        String lastEmited = this.defaultDateName(emited.split(" a ")[1].trim());
        // Fecha de publicación [1]
        animeInfo.getData().put("Publicado el", DataUtils.firstUpper(this.defaultDateName(emited.split(" a ")[0].trim())));
        // Fecha del último capítulo [2]
        animeInfo.setLastChapterDate(lastEmited);
      }
    }
    if (this.isValidData(duration) && !duration.equals("Desconocido")) {
      animeInfo.getData().put("Duracion", duration);
    }
    if (this.isValidData(quality)) {
      animeInfo.getData().put("quality", quality);
    }
    if (this.isValidData(alternativeTitles)) {
      animeInfo.setAlternativeTitles(AnimeUtils.specialDataKeys(alternativeTitles, this.specialAltTitles));
    }
    if (this.isValidData(history)) {
      animeInfo.setHistory(AnimeUtils.specialDataKeys(history, this.specialHistory));
    }
    if (this.isValidData(trailer)) {
      animeInfo.setTrailer("https://www.youtube.com/embed/" + trailer);
    }

    return animeInfo;
  }
  
  private Map<String, Object> getAlternativeTitlesJk(Document docJkanime) {
    Map<String, Object> alternativeTitles = new HashMap<>();
    Elements elements = docJkanime.select(".related_div");

    String currentKey = null;
    StringBuilder currentValue = new StringBuilder();

    for (Element element : elements) {
      for (Node node : element.childNodes()) {
        if (node instanceof Element && "b".equals(((Element) node).tag().getName())) {
          if (currentKey != null && currentValue.length() > 0) {
            String name = currentValue.toString().trim();
            if (!name.isEmpty()) {
              name = name.substring(0, 1).toUpperCase() + name.substring(1);
              alternativeTitles.put(currentKey, name);
            }
            currentValue.setLength(0);
          }
          currentKey = node.childNodes().isEmpty() ? "" : node.childNode(0).toString().trim();
        } else {
          currentValue.append(node.toString());
        }
      }
    }

    if (currentKey != null && currentValue.length() > 0) {
      alternativeTitles.put(currentKey, currentValue.toString().trim());
    }

    return alternativeTitles;
  }

  private Map<String, Object> getHistoryJK(Document docJkanime) {
    Elements allChilds = docJkanime.body().select(".aninfo").last().children();
    Map<String, Object> history = new LinkedHashMap<>();
    String currentKey = null;
    List<AnimeHistoryDTO> currentLinks = null;

    // Si hay elementos en el contenedor
    if (allChilds != null && !allChilds.isEmpty()) {
      // Lista de casos especiales
      Map<String, String> specialCases = new HashMap<>();
      for (SpecialCaseEntity sce : this.cacheUtils.getSpecialCases('k')) {
        specialCases.put(sce.getOriginal(), sce.getMapped());
      }
      // 1. En el primer ciclo: se guarda el titulo de la sublista y se ignora porque no hay nada que guardar
      // 2. En el segundo ciclo: se guarda la lista anterior en el mapa junto con su titulo (guardado en el anterior ciclo)
      // 3. En todos los ciclos: se inicializa la lista desde 0
      for (Element item : allChilds) {
        // Si es un titulo de una sublista (Precuela, Secuela, etc.)
        if (item.tagName().equals("h5")) {
          if (currentKey != null) {
            String name = this.animeUtils.specialNameOrUrlCases(null, currentKey, 'k', "getHistoryJK()");
            history.put(name, currentLinks);
          }
          currentKey = item.text();
          currentLinks = new ArrayList<>();
        // Si es un link
        } else if (item.tagName().equals("a") && currentLinks != null) {
          String[] parts = item.text().split("\\(");
          
          String name = parts[0].trim();
          String url = item.attr("href").replace(this.provider1, "").replace("/", "");
          name = this.animeUtils.specialNameOrUrlCases(specialCases, name, 'k', "getHistoryJK()");
          url = this.animeUtils.specialNameOrUrlCases(specialCases, url, 'k', "getHistoryJK()");

          AnimeHistoryDTO link = AnimeHistoryDTO.builder()
            .name(name)
            .url(url)
            .type(parts[parts.length - 1].replace(")", ""))
            .build();
          currentLinks.add(link);
        }
      }

      // Agregar la última lista al mapa, si existe
      if (currentKey != null) {
        if (!currentKey.contains("Trailer")) {
          history.put(currentKey, currentLinks);
        }
      }
    }

    return history;
  }

  private Object getSpecificKey(Elements keys, String key, boolean returnList) {
    String realValue = "";

    for (Element li : keys) {
      String realKey = li.text().split(":")[0].trim();
      Elements links = li.select("a");
      
      if (realKey.contains(key)) {
        if (!returnList) {
          if (links.isEmpty()) {
            return li.text().substring(li.text().indexOf(":") + 1).trim(); // "Emitido: 2019" -> "2019"
          }
        } else {
          return li.select("a");
        }
      }
    }

    return realValue;
  }
  
  // Funciones
  private String defaultDateName(String date) {
    date = date.replace(" de ", ", ");
    String partMonth = date.split(" ")[0];
    if (!this.tradMap.containsKey(partMonth)) {
      return date;
    } else {
      return date.replace(partMonth, this.tradMap.get(partMonth));
    }
  }

  // Validaciones
  private boolean isValidData(String data) {
    return data != null && !data.isEmpty() && !data.isBlank();
  }
  private boolean isValidData(Map<String, Object> data) {
    return data != null && !data.isEmpty();
  }
  private boolean isValidData(Integer data) {
    return data != null && data >= 0;
  }
  private boolean isValidData(Object data) {
    return data != null;
  }
    
}

package xyz.kiridepapel.fraxianimebackend.service;

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

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;

@Service
@Log
public class AnimeJkAnimeService {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  private Map<String, String> specialKeys = Map.ofEntries(
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
  private Map<String, String> specialHistory = Map.ofEntries(
    Map.entry("Precuela", "prequel"),
    Map.entry("Predecesor", "prequel"),
    Map.entry("Secuela", "sequel"),
    Map.entry("Version alternativa", "alternativeVersion"),
    Map.entry("Version completa", "completeVersion"),
    Map.entry("Adicional", "additional"),
    Map.entry("Resumen", "summary"),
    Map.entry("Personaje incluido", "includedCharacters"),
    Map.entry("Otro", "other")
  );

  public AnimeInfoDTO getAnimeInfo(AnimeInfoDTO animeInfo, Document docJkanime, String search) {
    Element mainJkanime = docJkanime.body().select(".contenido").first();
    Elements keys = docJkanime.select(".anime__details__text .anime__details__widget .aninfo ul li");

    // Obtener la información del anime de jkanime
    String alternativeName = mainJkanime.select(".anime__details__title span").first().text().trim();
    String jkanimeImgUrl = mainJkanime.select(".anime__details__pic").first().attr("data-setbg").trim();
    String synopsis = mainJkanime.select(".sinopsis").text().trim();
    Integer likes = Integer.parseInt(mainJkanime.select(".anime__details__content .vot").first().text().trim());
    String emited = this.getSpecificKey(keys, "Emitido");
    String duration = this.getSpecificKey(keys, "Duracion").replace("por episodio", "").trim();
    String quality = this.getSpecificKey(keys, "Calidad");
    Map<String, Object> alternativeTitles = this.getAlternativeTitles(docJkanime);
    Map<String, Object> history = this.getHistory(docJkanime);
    String trailer = mainJkanime.select(".animeTrailer").attr("data-yt");

    // Asignar la nueva información si es válida
    if (this.isValidData(alternativeName)) {
      animeInfo.setAlternativeName(alternativeName);
    }
    if (this.isValidData(jkanimeImgUrl)) {
      animeInfo.setImgUrl(jkanimeImgUrl);
    }
    if (this.isValidData(synopsis)) {
      animeInfo.setSinopsis(synopsis);
    }
    if (this.isValidData(likes)) {
      animeInfo.setLikes(likes);
    }
    if (this.isValidData(emited)) {
      animeInfo.getData().put("Publicado el", emited);
    }
    if (this.isValidData(duration) && !duration.equals("Desconocido")) {
      animeInfo.getData().put("Duracion", duration);
    }
    if (this.isValidData(quality)) {
      animeInfo.getData().put("quality", quality);
    }
    if (this.isValidData(alternativeTitles)) {
      animeInfo.setAlternativeTitles(AnimeUtils.specialDataKeys(alternativeTitles, this.specialKeys));
    }
    if (this.isValidData(history)) {
      animeInfo.setHistory(AnimeUtils.specialDataKeys(history, this.specialHistory));
    }
    if (this.isValidData(trailer)) {
      animeInfo.setTrailer("https://www.youtube.com/embed/" + trailer);
    }

    return animeInfo;
  }

  private Map<String, Object> getAlternativeTitles(Document docJkanime) {
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

  private Map<String, Object> getHistory(Document docJkanime) {
    Elements allChilds = docJkanime.body().select(".aninfo").last().children();
    Map<String, Object> history = new LinkedHashMap<>();
    String currentKey = null;
    List<String> currentLinks = null;

    // Si hay elementos en el contenedor
    if (allChilds != null && !allChilds.isEmpty()) {
      for (Element item : allChilds) {
        // Verificar si el elemento es un <h5>
        if (item.tagName().equals("h5")) {
          // Si ya se ha establecido un <h5>, guardar la lista anterior en el mapa
          if (currentKey != null) {
            history.put(currentKey, currentLinks);
          }
          // Inicializar una nueva lista para el nuevo <h5>
          currentKey = item.text();
          currentLinks = new ArrayList<>();
        // Verificar si el elemento es un <a>
        } else if (item.tagName().equals("a") && currentLinks != null) {
          // Añadir el texto del enlace a la lista actual
          currentLinks.add(item.text());
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

  private String getSpecificKey(Elements keys, String key) {
    String realValue = "";

    for (Element li : keys) {
      String realKey = li.text().split(":")[0].trim();
      Elements links = li.select("a");
      
      if (realKey.contains(key)) {
        if (links.isEmpty()) {
          return li.text().substring(li.text().indexOf(":") + 1).trim();
        }
      }
    }

    return realValue;
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
    
}

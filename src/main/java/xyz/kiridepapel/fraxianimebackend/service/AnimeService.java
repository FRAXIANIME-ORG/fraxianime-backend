package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;

@Service
@Log
public class AnimeService {
  @Value("${PROVEEDOR_JKANIME_URL}")
  private String proveedorJkanimeUrl;

  public AnimeInfoDTO getAnimeInfo(Document docJkanime) {
    try {
      Element contenido = docJkanime.body().select(".contenido").first();
      if (contenido == null) {
        throw new AnimeNotFound("Contenido del anime no disponible.");
      }

      String ytTrailerId = contenido.select(".anime__details__widget .animeTrailer").attr("data-yt").trim();
      if (ytTrailerId.isEmpty()) {
        ytTrailerId = null;
      }

      AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
        .name(contenido.select(".anime__details__title h3").first().text().trim())
        .imgUrl(contenido.select(".anime__details__pic").first().attr("data-setbg").trim())
        .sinopsis(contenido.select(".anime__details__text p.sinopsis").first().text().trim())
        .likes(Integer.parseInt(contenido.select(".anime__details__content .vot").first().text().trim()))
        .data(this.getData(docJkanime))
        .alternativeTitles(this.getAlternativeTitles(docJkanime))
        .ytTrailerId(ytTrailerId)
        .build();

      return animeInfo;
    } catch (Exception e) {
      log.warning("Error al obtener la información del anime: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private Map<String, Object> getData(Document docJkanime) {
    Map<String, Object> data = new HashMap<>();

    Elements elements = docJkanime.select(".anime__details__text .anime__details__widget .aninfo ul li");
    for (Element li : elements) {
      String key = li.text().split(":")[0].trim();
      Elements links = li.select("a");
      
      if (!links.isEmpty()) {
        // Caso en el que es una lista
        List<String> values = links.stream().map(Element::text).collect(Collectors.toList());
        data.put(key, values);
      } else {
        // Caso en el que es un solo elemento
        String value = li.text().substring(li.text().indexOf(":") + 1).trim();
        data.put(key, value);
      }
    }

    return data;
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
  
}

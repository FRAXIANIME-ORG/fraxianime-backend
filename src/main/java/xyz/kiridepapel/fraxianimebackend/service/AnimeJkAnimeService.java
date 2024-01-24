package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;

@Service
@Log
public class AnimeJkAnimeService {
  @Value("${PROVEEDOR_ANIMELIFE_URL}")
  private String proveedorAnimeLifeUrl;

  public AnimeInfoDTO getAnimeInfo(Document docJkanime) {
    try {
      Element contenido = docJkanime.body().select(".wrapper").first();
      if (contenido == null) {
        throw new AnimeNotFound("Contenido del anime no disponible.");
      }

      String trailer = contenido.select(".trailerbutton").attr("href").replace("watch?v=", "embed/");
      if (trailer.isEmpty()) {
        trailer = null;
      }

      AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
        .name(contenido.select(".entry-title").text().trim())
        .imgUrl(contenido.select(".thumbook img").attr("src").trim())
        .sinopsis(contenido.select(".synp p").text().trim())
        .trailer(trailer)
        .data(this.getAnimeData(docJkanime))
        // .recomendations(this.getRecomendations(docJkanime));
        .build();

      if (animeInfo.getData().get("Estado").equals("En emisión")) {
        animeInfo.setNextChapterDate(AnimeUtils.parseDate(animeInfo.getData().get("Actualizado el").toString(), 7));
      }

      animeInfo.setData(this.specialDataKeys(animeInfo.getData()));

      return animeInfo;
    } catch (Exception e) {
      log.warning("Error al obtener la información del anime: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private Map<String, Object> getAnimeData(Document docJkanime) {
    Elements items = docJkanime.select(".info-content .spe span");
    Map<String, Object> data = new HashMap<>();

    try {
      for (Element item : items) {
        Elements links = item.select("a");
        String key = item.text().split(":")[0].trim().replace(" en", " el");

        if (links.size() == 0 || links == null || links.isEmpty()) {
          if (item.select("time").size() > 0) {
            // Si hay un time, usarlo como value
            String value = item.select("time").text();
            data.put(key, value);
          } else {
            // Si no hay un time, usar el texto luego de los ":" como value
            String value = item.text().split(":")[1].trim();
            if (value.equals("TV")) {
              value = "Anime";
            }
            data.put(key, value);
          }
        } else {
          List<LinkDTO> subData = new ArrayList<>();
  
          for (Element link : links) {
            subData.add(LinkDTO.builder()
              .name(link.text().trim())
              .url(link.attr("href").replace(this.proveedorAnimeLifeUrl, ""))
              .build());
          }
  
          data.put(key, subData);
        }
      }
    } catch (Exception e) {
      log.info("Error al obtener la información del anime.");
    }

    return data;
  }

  private Map<String, Object> specialDataKeys(Map<String, Object> originalMap) {
    Map<String, String> specialKeys = Map.ofEntries(
      Map.entry("Publicado el", "publishedOn"),
      Map.entry("Tipo", "type"),
      Map.entry("Director", "director"),
      Map.entry("Censura", "censured"),
      Map.entry("Reparto", "cast"),
      Map.entry("Actualizado el", "updatedOn"),
      Map.entry("Año", "year"),
      Map.entry("Estado", "status"),
      Map.entry("Estudio", "studio")
    );

    Map<String, Object> newMap = new HashMap<>();

    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    return newMap;
  }

}

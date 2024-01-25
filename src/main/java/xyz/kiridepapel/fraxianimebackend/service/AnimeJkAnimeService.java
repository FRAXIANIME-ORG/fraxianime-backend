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

  private Map<String, String> specialKeys = Map.ofEntries(
    Map.entry("Publicado el", "emited"),
    Map.entry("Publicado", "emited"),
    Map.entry("Duracion", "duration"),
    Map.entry("Tipo", "type"),
    Map.entry("Director", "director"),
    Map.entry("Censura", "censured"),
    Map.entry("Reparto", "cast"),
    Map.entry("Actualizado el", "lastUpdate"),
    Map.entry("Año", "year"),
    Map.entry("Estado", "status"),
    Map.entry("Episodios", "chapters"),
    Map.entry("Estudio", "studio")
  );

  // public AnimeInfoDTO getAnimeInfo(Document docAnimeLife, Document docJkanime, String search) {
  public AnimeInfoDTO getAnimeInfo(Document docAnimeLife, String search) {
    try {
      Element contenido = docAnimeLife.body().select(".wrapper").first();

      if (contenido == null) {
        throw new AnimeNotFound("Contenido del anime no disponible.");
      }

      String trailer = contenido.select(".trailerbutton").attr("href").replace("watch?v=", "embed/");
      if (trailer.isEmpty()) {
        trailer = null;
      }

      AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
        .name(AnimeUtils.specialNameOrUrlCases(contenido.select(".entry-title").text().trim(), 'h'))
        .alternativeName(contenido.select(".entry-title").text().trim())
        .imgUrl(contenido.select(".thumbook img").attr("src").trim())
        .sinopsis(contenido.select(".synp p").text().trim())
        .trailer(trailer)
        .data(this.getAnimeData(docAnimeLife))
        // .recomendations(this.getRecomendations(docAnimeLife));
        .build();

      animeInfo = this.getChaptersInfo(animeInfo, docAnimeLife, search);

      animeInfo.setData(this.specialDataKeys(animeInfo.getData()));

      return animeInfo;
    } catch (Exception e) {
      log.warning("Error al obtener la información del anime: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private Map<String, Object> getAnimeData(Document docAnimeLife) {
    try {
      Map<String, Object> data = new HashMap<>();

      for (Element item : docAnimeLife.select(".info-content .spe span")) {
        Elements links = item.select("a");
        String key = item.text().split(":")[0].trim().replace(" en", " el");

        if (links.size() == 0 || links == null || links.isEmpty()) {
          if (item.select("time").size() > 0) {
            // Si hay un time, usar el text() como value
            String value = item.select("time").text();
            data.put(key, value);
          } else {
            // Si no hay un time, usar el texto luego de los ":" como value
            String[] values = item.text().split(":");
            if (values.length > 1) {
              String value = values[1].trim();
              if (value.equals("TV")) {
                value = "Anime";
              }
              data.put(key, value);
            }
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

      // Modificar la duración
      if (data.containsKey("Duracion")) {
        String duration = data.get("Duracion").toString();
        if (duration.contains("pero")) {
          duration = duration.replace(" pero ep.", "");
        }
        data.put("Duracion", duration);
      } else {
        data.put("Duracion", "25 min.");
      }
      
      // Establecer los géneros disponibles
      List<LinkDTO> genres = new ArrayList<>();
      for(Element genre : docAnimeLife.select(".genxed a")) {
        genres.add(LinkDTO.builder()
          .name(genre.text().trim())
          .url(genre.attr("href").replace(this.proveedorAnimeLifeUrl, ""))
          .build());
      }
      data.put("genres", genres);

      return data;
    } catch (Exception e) {
      log.info("Error al obtener la información del anime: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private AnimeInfoDTO getChaptersInfo(AnimeInfoDTO animeInfo, Document docAnimeLife, String search) {
    try {
      // Establecer la fecha del próximo capítulo
      if (animeInfo.getData().get("Estado").equals("En emisión")) {
        animeInfo.setNextChapterDate(AnimeUtils.parseDate(animeInfo.getData().get("Actualizado el").toString(), 7));
      }

      // Establecer la información del primer y último capítulo
      Element chapters = docAnimeLife.body().select(".lastend").first();
      if (chapters != null) {
        String firstStr = chapters.select(".epcurfirst").text()
          .replace("Episode", "")
          .replace("Episodio", "").trim();
        String lastStr = chapters.select(".epcurlast").text()
          .replace("Episode", "")
          .replace("Episodio", "").trim();
        
        if (firstStr.isEmpty() || firstStr.isEmpty()) {
          animeInfo.setFirstChapter(null);
          animeInfo.setLastChapter(null);
        }

        if (!firstStr.isEmpty() && firstStr.startsWith("0")) {
          log.info("real first: " + firstStr + " - " + firstStr.substring(1));
          animeInfo.setFirstChapter(Integer.parseInt(firstStr.substring(1)));
        } else {
          animeInfo.setFirstChapter(Integer.parseInt(firstStr));
        }
        if (!firstStr.isEmpty() && firstStr.startsWith("0")) {
          log.info("real last: " + lastStr + " - " + lastStr.substring(1));
          animeInfo.setLastChapter(Integer.parseInt(lastStr.substring(1)));
        } else {
          animeInfo.setLastChapter(Integer.parseInt(lastStr));
        }
      }

      return animeInfo;
    } catch (Exception e) {
      throw new AnimeNotFound("Error obteniendo la información de los capítulos del anime.");
    }
  }

  private Map<String, Object> specialDataKeys(Map<String, Object> originalMap) {
    Map<String, Object> newMap = new HashMap<>();

    for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
      String newKey = this.specialKeys.getOrDefault(entry.getKey(), entry.getKey());
      newMap.put(newKey, entry.getValue());
    }

    newMap.put("language", "Japonés");
    newMap.put("quality", "720p");

    return newMap;
  }

}

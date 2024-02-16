package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;

@Service
@Log
public class LfAnimeService {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  // Inyeccion de dependencias
  @Autowired
  private JkAnimeService jkAnimeService;
  @Autowired
  private TranslateService translateService;
  @Autowired
  private AnimeUtils animeUtils;
  // Variables
  private Map<String, String> specialKeys = Map.ofEntries(
    Map.entry("Tipo", "type"),
    Map.entry("Estudio", "studio"),
    Map.entry("Director", "director"),
    Map.entry("Reparto", "cast"),
    Map.entry("Censura", "censured"),
    Map.entry("Estado", "status"),
    Map.entry("Publicado el", "emited"),
    Map.entry("Episodios", "chapters"),
    Map.entry("Duracion", "duration")
  );
  
  @Cacheable("anime")
  public AnimeInfoDTO animeInfo(String search) {
    try {
      Document docJkanime = AnimeUtils.tryConnectOrReturnNull((this.providerJkanimeUrl + this.animeUtils.specialNameOrUrlCases(null, search, 'j')), 1);
      Document docAnimeLife = AnimeUtils.tryConnectOrReturnNull((this.providerAnimeLifeUrl + "anime/" + this.animeUtils.specialNameOrUrlCases(null, search, 'a')), 2);

      Element mainAnimeLife = docAnimeLife.body().select(".wrapper").first();

      if (mainAnimeLife == null) {
        throw new AnimeNotFound("Anime no disponible");
      }

      String trailer = mainAnimeLife.select(".trailerbutton").attr("href").replace("watch?v=", "embed/");
      if (trailer.isEmpty()) {
        trailer = null;
      }
      
      AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(null, mainAnimeLife.select(".entry-title").text().trim(), 'n'))
        .alternativeName(mainAnimeLife.select(".entry-title").text().trim())
        .imgUrl(mainAnimeLife.select(".thumbook img").attr("src").trim())
        .synopsis(mainAnimeLife.select(".synp p").text().trim())
        .alternativeTitles(this.getAlternativeTitles(docAnimeLife))
        .trailer(trailer)
        .lastChapterDate(mainAnimeLife.select(".info-content .spe span").last().select("time").text().replace(" de ", ", "))
        .data(this.getAnimeData(docAnimeLife))
        // .recomendations(this.getRecomendations(docAnimeLife));
        .build();

      // Si el capitulo existe en JkAnime, modificar la infomación con la de este
      boolean isMinDateInAnimeLf = false;
      Map<String, String> tradMap = Map.ofEntries(
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

      if (animeInfo.getLastChapterDate().equals("mayo 29, 2023")) {
        isMinDateInAnimeLf = true;
      }

      if (docJkanime != null) {
        animeInfo = this.jkAnimeService.getAnimeInfo(animeInfo, docJkanime, search, tradMap, isMinDateInAnimeLf);
      }

      // Establecer todo lo relacionado a los capítulos (info, numeros, fechas, etc.)
      animeInfo = this.setChaptersInfoAndList(animeInfo, docAnimeLife, tradMap, isMinDateInAnimeLf);

      // Busca y establece la sinopsis traducida
      animeInfo.setSynopsisEnglish(this.translateService.translate(animeInfo.getName(), animeInfo.getSynopsis()));

      // Modificar las keys obtenidas en data (español) -> (inglés)
      animeInfo.setData(AnimeUtils.specialDataKeys(animeInfo.getData(), this.specialKeys));

      return animeInfo;
    } catch (Exception e) {
      log.warning("Error 1: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private Map<String, Object> getAlternativeTitles(Document docAnimeLife) {
    Element altTitlesBlock = docAnimeLife.body().select(".alter").first();

    if (altTitlesBlock == null || altTitlesBlock.text().isEmpty()) {
      return null;
    } else {
      // String[] altTitles = altTitlesBlock.text().split(", ");
      List<String> altTitles = List.of(altTitlesBlock.text().split(","));
      Map<String, Object> alternativeTitles = new HashMap<>();

      altTitles.stream().forEach(title -> {
        alternativeTitles.put("others", title.trim());
      });

      return alternativeTitles;
    }
  }

  private Map<String, Object> getAnimeData(Document docAnimeLife) {
    try {
      Map<String, Object> data = new HashMap<>();

      for (Element item : docAnimeLife.select(".info-content .spe span")) {
        Elements links = item.select("a");
        String key = item.text().split(":")[0].trim().replace(" en", " el");
        
        if (key.equals("Publicado")) continue;

        if (links.size() == 0 || links == null || links.isEmpty()) {
          if (item.select("time").size() > 0) {
            // Si hay un time, usar el text() como value
            String value = item.select("time").text();
            data.put(key, value);
          } else {
            // Si no hay un time, usar el texto luego de los ":" como value
            String[] values = item.text().split(":");
            if (values.length > 1) {
              String value = values[1].replace(" pero ep.", "").trim();
              // Valores modificables de los values
              if (value.equals("TV")) {
                value = "Anime";
              }
              if (value.equals("Completada")) {
                value = "Finalizado";
              }
              data.put(key, value);
            }
          }
        } else {
          List<LinkDTO> subData = new ArrayList<>();
          for (Element link : links) {
            subData.add(LinkDTO.builder()
              .name(link.text().trim())
              .url(link.attr("href").replace(this.providerAnimeLifeUrl, ""))
              .build());
          }
          data.put(key, subData);
        }
      }

      // Eliminar keys innecesarias
      if (data.containsKey("Año")) {
        data.remove("Año");
      }
      if (data.containsKey("Actualizado el")) {
        data.remove("Actualizado el");
      }
      
      // Establecer los géneros disponibles
      List<LinkDTO> genres = new ArrayList<>();
      for(Element genre : docAnimeLife.select(".genxed a")) {
        genres.add(LinkDTO.builder()
          .name(genre.text().trim())
          .url(genre.attr("href").replace(this.providerAnimeLifeUrl, ""))
          .build());
      }
      data.put("genres", genres);

      return data;
    } catch (Exception e) {
      log.info("Error 2: " + e.getMessage());
      throw new AnimeNotFound("Anime no encontrado.");
    }
  }

  private AnimeInfoDTO setChaptersInfoAndList(AnimeInfoDTO animeInfo, Document docAnimeLife, Map<String, String> tradMap, boolean isMinDateInAnimeLf) {
    try {
      Elements chapters = docAnimeLife.body().select(".eplister ul li");
      List<ChapterDataDTO> chapterList = new ArrayList<>();
      
      if (chapters != null && !chapters.isEmpty()) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
        String date = animeInfo.getLastChapterDate();

        // Fecha del próximo capítulo
        if (animeInfo.getData().get("Estado").equals("En emisión")) {
          String ncd = DataUtils.parseDate(date, formatter, 7);
          ncd = ncd.substring(0, 1).toUpperCase() + ncd.substring(1);
          animeInfo.setNextChapterDate(ncd);
        }
        
        int index = 0;
        String lastChapterDate = "";
        for (Element element : chapters) {
          String[] chapterSplit = element.select(".epl-title").text().trim().split(" ");
          String chapter = chapterSplit[chapterSplit.length - 1];
          date = animeInfo.getLastChapterDate();

          // Si la fecha esta disponible en animeLife
          if (isMinDateInAnimeLf == false) {
            date = this.getChapterDateByIndex(date, formatter, (-1 * index++));
          } else {
            // Si la fecha no esta disponible en animeLife, obtener la fecha del primer capitulo en jkanime
            date = this.jkAnimeService.defaultDateName(tradMap, date);
            date = this.getChapterDateByIndex(date, formatter, ((chapters.size() - 1) + (-1 * index++)));
          }

          // Si es un número decimal, convertirlo a entero y sumarle 1
          if (chapter.contains(".")) {
            chapter = String.valueOf(Integer.parseInt(chapter.split("\\.")[0]) + 1);
          }
          // Si no es un número, asignarle 1 (Películas, Ovas, etc.)
          if (!chapter.matches("[0-9]+")) {
            chapter = String.valueOf(index);
          }

          // Establecer el ultimo capitulo y la cantidad de capítulos
          if (index == 1) {
            animeInfo.getData().put("Episodios", chapter);
            animeInfo.setLastChapter(Integer.parseInt(chapter));
            lastChapterDate = date.substring(0, 1).toUpperCase() + date.substring(1);
          }

          // Establecer la fecha de los capitulos
          chapterList.add(ChapterDataDTO.builder()
            .chapter(chapter)
            .date(date.substring(0, 1).toUpperCase() + date.substring(1))
            .build());
        }

        if (lastChapterDate != null) {
          animeInfo.setLastChapterDate(lastChapterDate);
        }

        // Invertir lista (primero el primer capitulo)
        Collections.reverse(chapterList);
      }

      animeInfo.setChapterList(chapterList);

      return animeInfo;
    } catch (Exception e) {
      throw new AnimeNotFound("1. Anime: " + e.getMessage());
    }
  }

  private String getChapterDateByIndex(String baseDate, DateTimeFormatter formatter, Integer index) {
    LocalDate date = LocalDate.parse(baseDate, formatter);
    String finalDate = date.plusDays(7 * index).format(formatter);
    return finalDate;
  }
}

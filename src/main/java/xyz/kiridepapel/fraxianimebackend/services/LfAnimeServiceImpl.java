package xyz.kiridepapel.fraxianimebackend.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.interfaces.ILfAnimeService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.exceptions.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.exceptions.DataExceptions.DataNotFoundException;

@Service
@Log
public class LfAnimeServiceImpl implements ILfAnimeService {
  // Variables estaticas
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  // Variables
  private Map<String, String> tradMap;
  private Map<String, String> specialKeys;
  // Inyeccion de dependencias
  private final JkAnimeServiceImpl jkAnimeService;
  private final TranslateServiceImpl translateService;
  private final CacheUtils cacheUtils;
  private final AnimeUtils animeUtils;

  // Constructor
  public LfAnimeServiceImpl(JkAnimeServiceImpl jkAnimeService,
      TranslateServiceImpl translateService, CacheUtils cacheUtils, AnimeUtils animeUtils) {
    this.jkAnimeService = jkAnimeService;
    this.translateService = translateService;
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
    this.specialKeys = Map.ofEntries(
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
  }
  
  @Cacheable(value = "anime")
  public AnimeInfoDTO anime(String search) {
    try {
      // Verifica si el anime está en los casos especiales pero invertidos (lo está buscando como está en AnimeLife y no en JkAnime)
      for (SpecialCaseEntity specialCase : this.cacheUtils.getSpecialCases('s')) {
        if (specialCase.getOriginal().equals(search)) {
          search = specialCase.getMapped();
          break;
        }
      }

      // Buscar en AnimeLife y JkAnime
      String animeUrlJk = this.animeUtils.specialNameOrUrlCases(null, search, 'j', "animeInfo()");
      String animeUrlLf = this.animeUtils.specialNameOrUrlCases(null, search, 'a', "animeInfo()");
      Document docJkanime = AnimeUtils.tryConnectOrReturnNull((this.providerJkanimeUrl + animeUrlJk), 1);
      Document docAnimeLife = AnimeUtils.tryConnectOrReturnNull((this.providerAnimeLifeUrl + "anime/" + animeUrlLf), 2);

      // Si no se encuentra el anime en AnimeLife, devolver un error
      Element mainAnimeLife = docAnimeLife.body().select(".wrapper").first();
      if (mainAnimeLife == null)
        throw new AnimeNotFound("Anime no disponible");
      
      // Si no encuentra el trailer en AnimeLife, devolver null
      String trailer = mainAnimeLife.select(".trailerbutton").attr("href").replace("watch?v=", "embed/");
      if (trailer.isEmpty())
        trailer = null;
      
      String name = mainAnimeLife.select(".entry-title").text().trim();
      AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(null, name, 'n', "animeInfo()"))
        .alternativeName(mainAnimeLife.select(".entry-title").text().trim())
        .imgUrl(mainAnimeLife.select(".thumbook img").attr("src").trim())
        .synopsis(mainAnimeLife.select(".synp p").text().trim())
        .alternativeTitles(this.getAlternativeTitles(docAnimeLife))
        .trailer(trailer)
        .lastChapterDate(mainAnimeLife.select(".info-content .spe span").last().select("time").text().replace(" de ", ", "))
        .data(this.getAnimeData(docAnimeLife))
        // .recomendations(this.getRecomendations(docAnimeLife))
        .build();

      // Si el capitulo existe en JkAnime, modificar la infomación con la de este
      boolean isMinDateInAnimeLf = false;
      boolean availableInJk = false;

      // AnimeLife salió en mayo 29, 2023, por lo que si es esa fecha, significa que no se conoce la fecha del último capítulo
      if (animeInfo.getLastChapterDate().equals("mayo 29, 2023") || animeInfo.getLastChapterDate().equals("mayo 28, 2023")) {
        isMinDateInAnimeLf = true;
      }

      // Si encuentra el ánime en JkAnime, modificar la información con la de JkAnime
      if (docJkanime != null) {
        animeInfo = this.jkAnimeService.getAnimeInfo(animeInfo, docJkanime, search, isMinDateInAnimeLf);
        availableInJk = true;
      }
      
      // Establecer todo lo relacionado a los capítulos (info, numeros, fechas, etc.)
      animeInfo = this.setChaptersInfoAndList(animeInfo, docAnimeLife, isMinDateInAnimeLf, availableInJk);

      // Busca y establece la sinopsis traducida
      animeInfo.setSynopsisEnglish(this.translateService.getTranslatedAndSave(animeInfo.getName(), animeInfo.getSynopsis(), "en"));

      // Modificar las keys obtenidas en data (español -> inglés)
      animeInfo.setData(AnimeUtils.specialDataKeys(animeInfo.getData(), this.specialKeys));

      return animeInfo;
    } catch (Exception e) {
      log.severe("LfAnimeService ERROR " + e.getMessage());
      throw new AnimeNotFound("Anime no disponible");
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

        if (links.size() == 0 || links == null || links.isEmpty()) {
          if (item.select("time").size() > 0) {
            String valueTime = item.select("time").text(); // Si hay un time, usar el text() como value
            valueTime = DataUtils.firstUpper(valueTime); // Primera letra en mayúscula de la fecha
            
            data.put(key, valueTime);
          } else {
            String[] values = item.text().split(":"); // Separar el texto por ":" y obtener el valor

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
      throw new DataNotFoundException("(getAnimeData): " + e.getMessage() + " ");
    }
  }

  private AnimeInfoDTO setChaptersInfoAndList(AnimeInfoDTO animeInfo, Document docAnimeLife, boolean isMinDateInAnimeLf, boolean availableInJk) {
    try {
      // Lista de capítulos
      Elements chapters = docAnimeLife.body().select(".eplister ul li");
      List<ChapterDataDTO> chapterList = new ArrayList<>();

      if (chapters != null && !chapters.isEmpty()) {
        String formatter = "MMMM d, yyyy";
        String date = animeInfo.getLastChapterDate();

        // * 1. Fecha del próximo capítulo
        if (animeInfo.getData().get("Estado").equals("En emisión")) {
          String ncd = DataUtils.parseDate(date, formatter, formatter, 7);
          ncd = DataUtils.firstUpper(ncd);
          animeInfo.setNextChapterDate(ncd);
        }
        
        // * 1. Establece la lista de capítulos
        int index = 0;
        for (Element element : chapters) {
          // Determina el capítulo
          String chapter = element.select(".epl-num").text().trim();
          // Si no es un número, asignarle 1 (Películas, Ovas, etc.)
          if (!chapter.matches("[0-9]+") && !chapter.contains(".")) {
            chapter = String.valueOf(index + 1);
          } else {
            chapter = String.valueOf(Float.parseFloat(chapter)).replace(".0", "");
          }

          chapterList.add(ChapterDataDTO.builder()
            .chapter(chapter)
            .type(DataUtils.firstUpper(element.select(".epl-sub .status").text().trim().toLowerCase()))
            .build());
        }

        // * 2. Verifica si la lista está desordenada
        boolean isUnsorted = false;
        for (int i = 1; i < chapterList.size(); i++) {
          if (Float.parseFloat(chapterList.get(i).getChapter()) > Float.parseFloat(chapterList.get(i - 1).getChapter())) {
            isUnsorted = true;
            break;
          }
        }
        
        // * 2. Ordenar la lista de mayor a menor capítulo solo si está desordenada (0: Capítulo 1090)
        if (isUnsorted) {
          Collections.sort(chapterList, (c1, c2) -> Float.compare(Float.parseFloat(c2.getChapter()), Float.parseFloat(c1.getChapter())));
        }

        // * 1. Asigna las fechas a la lista de capítulos
        index = 0;
        String lastChapterDate = null;
        for(ChapterDataDTO element : chapterList) {
          // Si la fecha esta disponible en animeLife o no esta disponible en jkanime
          date = animeInfo.getLastChapterDate();
          if (isMinDateInAnimeLf == false && availableInJk == true) {
            date = DataUtils.parseDate(date, formatter, formatter, (7 * (-1 * index++)));
          } else {
            // Si la fecha no esta disponible en animeLife, obtener la fecha del primer capitulo en jkanime
            date = this.defaultDateName(date);
            date = DataUtils.parseDate(date, formatter, formatter, (7 * ((chapters.size() - 1) + (-1 * index++))));
          }

          // Establecer el ultimo capitulo y la cantidad de capítulos
          if (index == 1) {
            animeInfo.getData().put("Episodios", element.getChapter());
            animeInfo.setLastChapter(String.valueOf(element.getChapter()));
            lastChapterDate = DataUtils.firstUpper(date);
          }

          element.setDate(DataUtils.firstUpper(date));
        }
        
        // * 2. Ordenar la lista de menor a mayor capítulo (0: Capítulo 1)
        Collections.reverse(chapterList);
        
        if (lastChapterDate != null) {
          animeInfo.setLastChapterDate(lastChapterDate);
        }
      }

      animeInfo.setChapterList(chapterList);

      return animeInfo;
    } catch (Exception e) {
      throw new DataNotFoundException("(setChaptersInfoAndList): " + e.getMessage() + " ");
    }
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
}

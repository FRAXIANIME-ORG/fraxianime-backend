package xyz.kiridepapel.fraxianimebackend.service.anime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.TopDTO;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.service.general.TranslateService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
@Log
public class JkTopService {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  // Inyección de dependencias
  @Autowired
  private TranslateService translateService;
  @Autowired
  private AnimeUtils animeUtils;
  @Autowired
  private CacheUtils cacheUtils;
  // Variables
  public static final List<String> seasonNames = List.of(
    "actual",
    "spring",
    "summer",
    "fall",
    "winter"
  );
  public static final Map<String, String> seasonNamesInJk = Map.of(
    "actual", "",
    "spring", "Primavera",
    "summer", "Verano",
    "fall", "Otoño",
    "winter", "Invierno"
  );

  @Cacheable(value = "pastYearsTop", key = "#year.concat('-').concat(#season)")
  public TopDTO pastYearsCacheTop(String year, String season) {
    try {
      return this.constructTop(year, seasonNamesInJk.get(season));
    } catch (Exception e) {
      log.warning(e.getMessage() + " - " + e.getStackTrace());
      throw new DataNotFoundException("Ocurrió un error al obtener el top");
    }
  }
  
  @Cacheable(value = "actualYearTop", key = "#year.concat('-').concat(#season)")
  public TopDTO actualYearCacheTop(String year, String season) {
    try {
      return this.constructTop(year, seasonNamesInJk.get(season));
    } catch (Exception e) {
      log.warning(e.getMessage() + " - " + e.getStackTrace());
      throw new DataNotFoundException("Ocurrió un error al obtener el top");
    }
  }

  public TopDTO constructTop(String year, String season) {
    // Armar la URL
    String uri = this.providerJkanimeUrl;
    Integer actualYear = DataUtils.getLocalDateTimeNow(this.isProduction).getYear();
    if (year.equals(actualYear.toString()) && season.isEmpty()) {
      uri += "top";
    }
    else if (!year.equals(actualYear.toString()) && !season.isEmpty() || !season.isBlank()) {
      uri += "top/?fecha=" + year + "&temporada=" + season;
    }
    else {
      throw new DataNotFoundException("Se debe especificar un año y una temporada");
    }

    Document docJkAnime = AnimeUtils.tryConnectOrReturnNull(uri, 1);
    
    if (docJkAnime == null) {
      throw new DataNotFoundException("No se pudo conectar con los proveedores");
    }

    // Mapa de casos especiales donde JkAnime se acopla a AnimeLife
    Map<String, String> mapListTypeJk = new HashMap<>();
    this.cacheUtils.getSpecialCases('k').forEach(sce -> mapListTypeJk.put(sce.getOriginal(), sce.getMapped()));

    // Listas
    List<TopDataDTO> topDataList = new ArrayList<>();
    Elements topElements = docJkAnime.body().select(".contenido .container div.list");

    for (Element element : topElements) {
      String name = element.select(".timg").attr("title");
      String imgUrl = element.select("img").attr("src");
      String likes = element.select(".rank").text().trim();
      String position = element.select("#animinfo .portada-title").text();
      String url = element.select("#animinfo a").attr("href");
      String type = element.select("#animinfo .title").text();
      String chapters = element.select("#animinfo .title").text();
      String synopsis = element.select("#animinfo p").text();
      
      // Replace cases
      position = position.trim().replace("#", "").split(" ")[0];
      url = url.split("/")[3];
      type = type.split("/")[0].trim().replace("Serie", "Anime");
      chapters = chapters.split("/")[1].replace("Eps", "").trim();
      Integer chaptersInt = chapters.matches("\\d+") ? Integer.parseInt(chapters) : null;

      topDataList.add(TopDataDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "getTop()"))
        .imgUrl(imgUrl)
        .likes(Integer.parseInt(likes))
        .position(Integer.parseInt(position))
        .url(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, url, 'k', "getTop()"))
        .type(type)
        .chapters(chaptersInt)
        .synopsis(synopsis)
        .synopsisEnglish(this.translateService.getTranslated(name, "en"))
        .build()
      );
    }

    return TopDTO.builder()
      .top(topDataList)
      .build();
  }
}

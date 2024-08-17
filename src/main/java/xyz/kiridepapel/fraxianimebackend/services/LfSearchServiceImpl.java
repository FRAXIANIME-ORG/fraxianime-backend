package xyz.kiridepapel.fraxianimebackend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.SearchDTO;
import xyz.kiridepapel.fraxianimebackend.exceptions.AnimeExceptions.SearchException;
import xyz.kiridepapel.fraxianimebackend.interfaces.ILfSearchService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;

@Service
@Log
public class LfSearchServiceImpl implements ILfSearchService {
  // Variables estaticas
  @Value("${PROVIDER_2}")
  private String provider2;
  // Inyección de dependencias
  private final CacheUtils cacheUtils;
  private final AnimeUtils animeUtils;

  // Constructor
  public LfSearchServiceImpl(CacheUtils cacheUtils, AnimeUtils animeUtils) {
    this.cacheUtils = cacheUtils;
    this.animeUtils = animeUtils;
  }

  public SearchDTO searchAnimes(String anime, Integer page) {
    try {
      anime = this.verySpecialAndManuallyCases(anime);
      String searchUrl = this.provider2 + "page/" + page + "/?s="
          + anime.replace(":", "%3A").replace("_", "+");

      SearchDTO searchDTO = new SearchDTO();
      Document docAnimeLife = AnimeUtils.tryConnectOrReturnNull(searchUrl, 2);

      if (docAnimeLife != null && docAnimeLife.body().select(".listupd center h3").first() == null) {
        Elements animes = docAnimeLife.body().select(".listupd article");

        List<AnimeDataDTO> searchList = new ArrayList<>();
        Integer itemsToShow = animes.size();

        // Casos especiales
        Map<String, String> specialCasesNames = new HashMap<>();
        Map<String, String> specialCasesUrls = new HashMap<>();
        this.cacheUtils.getSpecialCases('n').forEach(hsce -> specialCasesNames.put(hsce.getOriginal(), hsce.getMapped()));
        this.cacheUtils.getSpecialCases('s').forEach(hsce -> specialCasesUrls.put(hsce.getOriginal(), hsce.getMapped()));

        // Search list
        for (int i = 0; i < itemsToShow; i++) {
          String name = animes.get(i).select(".tt h2").text();
          String imgUrl = animes.get(i).select("img").attr("src");
          String url = animes.get(i).select("a").attr("href");
          String state = animes.get(i).select(".epx").text();
          String type = animes.get(i).select(".typez").text();
          
          // Casos especiales manipulados
          name = AnimeUtils.removeRareCharactersFromName(name);
          imgUrl = imgUrl.replace("?resize=247,350", "");
          url = url.replace((provider2 + "anime/"), "").replaceAll("/$", "");
          state = state.replace("Completada", "Finalizado");
          type = type.replace("TV", "Anime");

          // Casos especiales de la base de datos
          name = this.animeUtils.specialNameOrUrlCases(specialCasesNames, name, 'H', "searchAnimes()");
          url = this.animeUtils.specialNameOrUrlCases(specialCasesUrls, url, 'H', "searchAnimes()");

          searchList.add(AnimeDataDTO.builder()
            .name(name)
            .imgUrl(imgUrl)
            .url(url)
            .state(state)
            .type(type)
            .build());
        }

        // Determinar la ultima pagina
        Elements pages = docAnimeLife.body().select(".pagination .page-numbers");

        if (pages != null && pages.size() > 0) {
          // Eliminar los botones / textos de "Anterior" y "Siguiente"
          Pattern pattern = Pattern.compile("Siguiente", Pattern.CASE_INSENSITIVE);
          pages.removeIf(p -> pattern.matcher(p.text()).find());

          searchDTO.setLastPage(Integer.parseInt(pages.last().text().trim()));
        }

        // Colocar los animes en emisión al principio
        searchList.sort((a, b) -> {
          if (a.getState() != null && a.getState().equals("En emisión")) {
            return -1;
          } else {
            return 0;
          }
        });

        searchDTO.setSearchList(searchList);

        return searchDTO;

      } else {
        searchDTO.setMessage("No se encontraron resultados.");
        return searchDTO;
      }
    } catch (Exception e) {
      log.severe("Error al buscar animes: " + e.getMessage());
      throw new SearchException("Ocurrió un error al buscar los animes.");
    }
  }

  private String verySpecialAndManuallyCases(String anime) {
    if (anime.contains("ore_dake_le")) {
      return "solo_leveling";
    }

    return anime;
  }
}

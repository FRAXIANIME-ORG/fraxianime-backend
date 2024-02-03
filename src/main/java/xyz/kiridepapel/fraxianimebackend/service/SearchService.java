package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.InvalidSearch;
import xyz.kiridepapel.fraxianimebackend.repository.SpecialCaseRepository;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
public class SearchService {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;

  @Autowired
  private SpecialCaseRepository specialCaseRepository;

  public Map<String, Object> searchAnimes(String anime, Integer page, Integer maxItems) {
    anime = this.verySpecialAndManuallyCases(anime);
    String searchUrl = this.providerAnimeLifeUrl + "page/" + page + "/?s=" + anime.replace(":", "%3A").replace("_", "+");

    Map<String, Object> searchMap = new HashMap<>();
    Document docAnimeLife = DataUtils.tryConnectOrReturnNull(searchUrl, 2);

    if (docAnimeLife != null && docAnimeLife.body().select(".listupd center h3").first() == null) {
      Elements animes = docAnimeLife.body().select(".listupd article");

      List<AnimeDataDTO> searchList = new ArrayList<>();
      Integer itemsToShow = animes.size();

      if (maxItems != null && (maxItems <= itemsToShow && maxItems > 0)) {
        itemsToShow = maxItems;
      }

      // Special cases
      Map<String, String> specialCases = this.specialCaseRepository.findByTypes('n', 's').stream()
        .collect(Collectors.toMap(SpecialCaseEntity::getOriginal, SpecialCaseEntity::getMapped));

      // Search list
      for (int i = 0; i < itemsToShow; i++) {
        String name = animes.get(i).select(".tt h2").text();
        String url = animes.get(i).select("a").attr("href")
          .replace((providerAnimeLifeUrl + "anime/"), "")
          .replaceAll("/$", "");

        if (specialCases.containsKey(name)) {
          name = name.replace(name, specialCases.get(name));
        }
        if (specialCases.containsKey(url)) {
          url = url.replace(url, specialCases.get(url));
        }

        AnimeDataDTO data = AnimeDataDTO.builder()
            .name(name)
            .imgUrl(animes.get(i).select("img").attr("src").replace("?resize=247,350", ""))
            .url(url)
            .state(animes.get(i).select(".epx").text().replace("Completada", "Finalizado"))
            .type(animes.get(i).select(".typez").text().replace("TV", "Anime"))
            .build();

        searchList.add(data);
      }

      // Max page
      Elements pages = docAnimeLife.body().select(".pagination a");

      if (pages != null && pages.size() > 0) {
        pages.remove(pages.size() - 1);
        searchMap.put("lastPage", Integer.parseInt(pages.last().text()));
      }

      searchMap.put("searchList", searchList);

      return searchMap;
    } else {
      searchMap.put("message", "No se encontraron resultados.");
      return searchMap;
    }
  }

  private String verySpecialAndManuallyCases(String anime) {
    if (anime.contains("ore_dake_le")) {
      return "solo_leveling";
    }

    return anime;
  }
}

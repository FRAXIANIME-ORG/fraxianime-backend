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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.DirectoryOptionsDTO;
import xyz.kiridepapel.fraxianimebackend.exceptions.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.interfaces.ILfDirectoryService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;

@Service
public class LfDirectoryServiceImpl implements ILfDirectoryService {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  // Variables
  private List<String> removeNamesList = List.of(
    // Varios
    "Todo",
    // Tipo
    "BD",
    "Music",
    // Idioma
    "RAW",
    "LAT",
    // Orden
    "Por Defecto"
  );
  private Map<String, String> modifiedNames = Map.ofEntries(
    // Estudio
    Map.entry("Children's Playground Entertainment", "Children's Playground Ent."),
    // Estado
    Map.entry("Completada", "Finalizados"),
    Map.entry("En Emisión", "En emisión"),
    // Tipo
    Map.entry("TV Series", "Animes"),
    Map.entry("OVA", "OVAs"),
    Map.entry("Live Action", "Live actions"),
    Map.entry("ONA", "ONAs"),
    // Orden
    Map.entry("A-Z", "Descendente (A-Z)"),
    Map.entry("Z-A", "Ascendente (Z-A)"),
    Map.entry("Ultima Actualizacion", "Últimos actualizados"),
    Map.entry("Ultimos Agregados", "Últimos agregados"),
    Map.entry("", "")
  );
  // Inyección de dependencias
  private final CacheUtils cacheUtils;
  private final AnimeUtils animeUtils;
  private final CacheManager cacheManager;

  // Constructor
  public LfDirectoryServiceImpl(CacheUtils cacheUtils, AnimeUtils animeUtils, CacheManager cacheManager) {
    this.cacheUtils = cacheUtils;
    this.animeUtils = animeUtils;
    this.cacheManager = cacheManager;
  }

  @Cacheable(value = "directory", key = "#options")
  public DirectoryOptionsDTO directoryOptions(String options) {
    Document docAnimeLife = AnimeUtils.tryConnectOrReturnNull((this.providerAnimeLifeUrl + "anime"), 2);

    if (docAnimeLife == null) {
      throw new DataNotFoundException("No se pudo conectar con los proveedores");
    }

    DirectoryOptionsDTO directoryOptionsDTO = new DirectoryOptionsDTO();
    Elements filters = docAnimeLife.body().select(".quickfilter .dropdown");

    filters.forEach(filter -> {
      String optionName = filter.select("ul li").first().select("input").attr("name").replace("[]", "");
      Elements optionsElements = filter.select("ul li");
      List<LinkDTO> optionsList = new ArrayList<>();

      optionsElements.forEach(option -> {
        String name = option.select("label").text();
        String url = option.select("input").attr("value");

        if (!removeNamesList.contains(name)) {
          LinkDTO linkDTO = LinkDTO.builder()
            .name(name)
            .url(url)
            .build();
          
          if (modifiedNames.containsKey(name)) {
            linkDTO.setName(modifiedNames.get(name));
          }

          optionsList.add(linkDTO);
        }
      });
      
      if (optionName.equals("genre")) directoryOptionsDTO.setGenres(optionsList);
      else if (optionName.equals("season")) {
        Collections.reverse(optionsList);
        directoryOptionsDTO.setSeasons(optionsList);
      }
      else if (optionName.equals("studio")) directoryOptionsDTO.setStudios(optionsList);
      else if (optionName.equals("status")) directoryOptionsDTO.setStatus(optionsList);
      else if (optionName.equals("type")) directoryOptionsDTO.setTypes(optionsList);
      else if (optionName.equals("sub")) directoryOptionsDTO.setSubs(optionsList);
      else if (optionName.equals("order")) directoryOptionsDTO.setOrders(optionsList);
    });

    return directoryOptionsDTO;
  }

  @Cacheable(value = "directory", key = "#uri")
  public List<AnimeDataDTO> saveLongDirectoryAnimes(String uri) {
    return this.directoryAnimes(uri);
  }
  
  @SuppressWarnings("unchecked")
  public List<AnimeDataDTO> constructDirectoryAnimes(String uri) {
    // Obtiene el directorio si está en caché
    List<AnimeDataDTO> directoryCache = CacheUtils.searchFromCache(cacheManager, List.class, "directory", uri);
    if (directoryCache != null) {
      return directoryCache;
    }
    // Si no está en caché, lo obtiene y lo guarda
    return this.directoryAnimes(uri);
  }

  private List<AnimeDataDTO> directoryAnimes(String uri) {
    Document docAnimeLife = AnimeUtils.tryConnectOrReturnNull((this.providerAnimeLifeUrl + "anime/" + uri), 2);

    if (docAnimeLife == null) {
      throw new DataNotFoundException("No se pudo conectar con los proveedores");
    }

    // Casos especiales
    Map<String, String> specialCasesNames = new HashMap<>();
    Map<String, String> specialCasesUrls = new HashMap<>();
    this.cacheUtils.getSpecialCases('n').forEach(hsce -> specialCasesNames.put(hsce.getOriginal(), hsce.getMapped()));
    this.cacheUtils.getSpecialCases('s').forEach(hsce -> specialCasesUrls.put(hsce.getOriginal(), hsce.getMapped()));
    
    // Listas
    Elements animes = docAnimeLife.body().select(".listupd .bs");
    List<AnimeDataDTO> animesList = new ArrayList<>();

    // Items de las listas
    for (Element item : animes) {
      String name = item.select(".tt h2").text();
      String imgUrl = item.select("img").attr("src");
      String url = item.select("a").attr("href");
      String state = item.select(".epx").text();
      String type = item.select(".typez").text();
      
      // Casos especiales manipulados
      name = AnimeUtils.removeRareCharactersFromName(name);
      imgUrl = imgUrl.replace("?resize=247,350", "");
      url = url.replace((providerAnimeLifeUrl + "anime/"), "").replaceAll("/$", "");
      state = state.replace("Completada", "Finalizado");
      type = type.replace("TV", "Anime");
      
      // Casos especiales de la base de datos
      name = this.animeUtils.specialNameOrUrlCases(specialCasesNames, name, 'H', "directoryAnimes()");
      url = this.animeUtils.specialNameOrUrlCases(specialCasesUrls, url, 'H', "directoryAnimes()");

      animesList.add(AnimeDataDTO.builder()
        .name(name)
        .imgUrl(imgUrl)
        .url(url)
        .state(state)
        .type(type)
        .build());
    }

    return animesList;
  }
  
}

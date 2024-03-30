package xyz.kiridepapel.fraxianimebackend.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dtos.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exceptions.AnimeExceptions.InvalidSearch;
import xyz.kiridepapel.fraxianimebackend.interfaces.*;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@RestController
@RequestMapping("/api/v1/anime")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
public class AnimeController {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  // Inyección de dependencias (instanciadas en el constructor para evitar problemas de reflexión)
  private final IJkLfHomeService iJkLfHomeService;
  private final ILfDirectoryService iLfDirectoryService;
  private final IJkScheduleService iJkScheduleService;
  private final IJkTopService iJkTopService;
  private final ILfAnimeService iLfAnimeService;
  private final ILfChapterService iLfChapterService;
  private final ILfSearchService iLfSearchService;
  private final MessageSource msg;
  // Variables
  private List<String> allowedOrigins;
  private List<String> allowedSeasons;

  // Constructor
  public AnimeController(IJkLfHomeService iJkLfHomeService, ILfDirectoryService iLfDirectoryService,
      IJkScheduleService iJkScheduleService, IJkTopService iJkTopService, ILfAnimeService iLfAnimeService,
      ILfChapterService iLfChapterService, ILfSearchService iLfSearchService, MessageSource msg) {
    this.iJkLfHomeService = iJkLfHomeService;
    this.iLfDirectoryService = iLfDirectoryService;
    this.iJkScheduleService = iJkScheduleService;
    this.iJkTopService = iJkTopService;
    this.iLfAnimeService = iLfAnimeService;
    this.iLfChapterService = iLfChapterService;
    this.iLfSearchService = iLfSearchService;
    this.msg = msg;
  }

  // Inicialización
  @PostConstruct
  private void init() {
    this.allowedOrigins = Arrays.asList(frontendUrl);
    this.allowedSeasons = Arrays.asList("actual", "spring", "summer", "fall", "winter");
  }

  @GetMapping("/locale")
  public ResponseEntity<?> locate(
      HttpServletRequest request, @RequestParam(value = "lang", defaultValue = "en") String lang) {
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(lang);

    Locale locale;
    try {
      locale = new Locale(lang);
    } catch (Exception e) {
      locale = new Locale("en");
    }

    String greeting = msg.getMessage("greeting", null, locale);
    return new ResponseEntity<>(greeting, HttpStatus.OK);
  }

  @GetMapping("/home")
  public ResponseEntity<?> home(HttpServletRequest request) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    // Obtener datos y respuesta
    return new ResponseEntity<>(this.iJkLfHomeService.home(), HttpStatus.OK);
  }
  
  @GetMapping("/directory/options")
  public ResponseEntity<?> directoryOptions(HttpServletRequest request) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    // Obtener datos y respuesta
    return new ResponseEntity<>(this.iLfDirectoryService.directoryOptions("options"), HttpStatus.OK);
  }

  @GetMapping("/directory/{page}")
  public ResponseEntity<?> directoryAnimes(HttpServletRequest request,
      @PathVariable("page") Integer page,
      @RequestParam(required = false, value = "genre", defaultValue = "") List<String> genre,
      @RequestParam(required = false, value = "season", defaultValue = "") List<String> season,
      @RequestParam(required = false, value = "studio", defaultValue = "") List<String> studio,
      @RequestParam(required = false, value = "status", defaultValue = "") String status,
      @RequestParam(required = false, value = "type", defaultValue = "") String type,
      @RequestParam(required = false, value = "sub", defaultValue = "") String sub,
      @RequestParam(required = false, value = "order", defaultValue = "") String order) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));

    // Construir URI
    String uri = "?page=" + page;
    for (String element : genre) uri += "&genre=" + element;
    for (String element : season) uri += "&season=" + element;
    for (String element : studio) uri += "&studio=" + element;
    if (status != null && !status.isEmpty()) uri += "&status=" + status;
    if (type != null && !type.isEmpty()) uri += "&type=" + type;
    if (sub != null && !sub.isEmpty()) uri += "&sub=" + sub;
    if (order != null && !order.isEmpty()) uri += "&order=" + order;

    // Obtener datos y respuesta
    return new ResponseEntity<>(this.iLfDirectoryService.constructDirectoryAnimes(uri), HttpStatus.OK);
  }

  @GetMapping("/schedule")
  public ResponseEntity<?> schedule(HttpServletRequest request) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    // Obtener datos y respuesta
    return new ResponseEntity<>(this.iJkScheduleService.getSchedule("list"), HttpStatus.OK);
  }

  @GetMapping("/top")
  public ResponseEntity<?> top(HttpServletRequest request,
      @RequestParam(required = false, value = "year") String year,
      @RequestParam(required = false, value = "season") String season) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    
    // Valores por defecto (actual)
    Integer actualYear = DataUtils.getLocalDateTimeNow(this.isProduction).getYear();
    if (year == null || year.isEmpty() || year.isBlank()) year = actualYear.toString();
    if (season == null || season.isEmpty() || season.isBlank()) season = "actual";

    // Verifica que sea un número de 4 dígitos, entre 2020 y el año actual y que la temporada sea válida
    if (year.matches("\\d{4}") && this.allowedSeasons.contains(season) && Integer.parseInt(year) >= 2020 && Integer.parseInt(year) <= actualYear) {      
      // Obtener datos
      if (year.equals(actualYear.toString())) {
        return new ResponseEntity<>(this.iJkTopService.actualYearCacheTop(year, season), HttpStatus.OK);
      } else {
        return new ResponseEntity<>(this.iJkTopService.pastYearsCacheTop(year, season), HttpStatus.OK);
      }
    } else {
      return new ResponseEntity<>(new ResponseDTO("Año o temporada inválidos", 400), HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/{anime}")
  public ResponseEntity<?> anime(HttpServletRequest request,
      @PathVariable("anime") String anime) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);

    // Obtener datos
    AnimeInfoDTO animeInfo = this.iLfAnimeService.anime(anime);

    // Respuesta
    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado", HttpStatus.OK);
    }
  }

  @GetMapping("/{anime}/{chapter}")
  public ResponseEntity<?> chapter(HttpServletRequest request,
      @PathVariable("anime") String anime,
      @PathVariable("chapter") String chapter) {
        
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);
    DataUtils.verifySQLInjection(chapter);

    // Restricciones cuando el capítulo es XX-X
    if (chapter.contains("-")) {
      Integer chapterNumberPart = Integer.parseInt(chapter.split("-")[0]);
      Integer chapterSecondPart = Integer.parseInt(chapter.split("-")[1]);
      // Retorna error si el capítulo es negativo o != XX-2 o XX-5
      if (chapterNumberPart < 0 || (chapterSecondPart != 2 && chapterSecondPart != 5)) {
        return new ResponseEntity<>("El capítulo solicitado no es válido", HttpStatus.BAD_REQUEST);
      }
    };

    // Restricciones cuando el capítulo es XX
    if (!chapter.contains("-") && Integer.parseInt(chapter) < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido", HttpStatus.BAD_REQUEST);
    }

    // Obtener datos
    ChapterDTO chapterInfo = this.iLfChapterService.constructChapter(anime, chapter);

    // Respuesta
    if (DataUtils.isNotNullOrEmpty(chapterInfo)) {
      return new ResponseEntity<>(chapterInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado", HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/search/{anime}/{page}")
  public ResponseEntity<?> searchAnimesWithoutMax(HttpServletRequest request,
      @PathVariable(value = "anime", required = true) String anime,
      @PathVariable(value = "page", required = true) Integer page) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);

    // Restricciones
    if (page == null || page < 1 || page > 300) {
      throw new InvalidSearch("La página solicitada no es válida.");
    }

    // Obtener datos y respuesta
    return new ResponseEntity<>(this.iLfSearchService.searchAnimes(anime, page), HttpStatus.OK);
  }

}

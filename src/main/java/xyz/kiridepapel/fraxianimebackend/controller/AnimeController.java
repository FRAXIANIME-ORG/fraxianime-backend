package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.SearchDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.InvalidSearch;
import xyz.kiridepapel.fraxianimebackend.service.LfAnimeService;
import xyz.kiridepapel.fraxianimebackend.service.HomeService;
import xyz.kiridepapel.fraxianimebackend.service.LfSearchService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.service.LfChapterService;

@RestController
@RequestMapping("/api/v1/anime")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
public class AnimeController {
  // Variables estaticas
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  // Variables
  private List<String> allowedOrigins;
  // Inyección de dependencias
  @Autowired
  private HomeService homePageService;
  @Autowired
  private LfAnimeService lfAnimeService;
  @Autowired
  private LfChapterService chapterService;
  @Autowired
  private LfSearchService searchService;
  // Language
  @Autowired
  private MessageSource msg;

  @PostConstruct
  public void init() {
    this.allowedOrigins = Arrays.asList(frontendUrl);
  }

  @GetMapping("/test")
  public ResponseEntity<?> test1() {
    // Fecha exacta con tiempo UTC y 5 horas menos (Hora de Perú)
    String date = "Ayer"; // Ayer, dd/MM
    Date today = new Date();
    // today.setTime(today.getTime() - 18000000); // 5 horas
    // today.setTime(today.getTime() - 14400000); // 4 horas
    today.setTime(today.getTime() + 10800000); // 3 horas
    // today.setTime(today.getTime() - 7200000); // 2 horas
    // today.setTime(today.getTime() - 3600000); // 1 hora
    Calendar nowCal = Calendar.getInstance();
    // restarle 3 horas a nowCal
    nowCal.setTime(today);

    // Si la hora es >= 19 < 0, entonces es "Hoy"
    // Si es la 1 am, entonces es "Ayer"
    if (date.equals("Hoy") || (date.equals("Ayer") &&
      nowCal.get(Calendar.HOUR_OF_DAY) >= 19 || nowCal.get(Calendar.HOUR_OF_DAY) < 0)) {
      date = "Hoy";
    }

    return new ResponseEntity<>("today: " + today + " - nowCal: " + nowCal.get(Calendar.HOUR_OF_DAY) + " - newDate: " + date, HttpStatus.OK);
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
  public ResponseEntity<?> homePage(HttpServletRequest request) {
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));

    HomePageDTO animes = this.homePageService.homePage();

    if (DataUtils.isNotNullOrEmpty(animes.getSliderAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
        DataUtils.isNotNullOrEmpty(animes.getAnimesProgramming()) &&
        DataUtils.isNotNullOrEmpty(animes.getNextAnimesProgramming()) &&
        DataUtils.isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
        DataUtils.isNotNullOrEmpty(animes.getTopAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getLatestAddedList())) {
      return new ResponseEntity<>(animes, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new ResponseDTO("No se pudo recuperar todos los datos", 404), HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/{anime}")
  public ResponseEntity<?> animeInfo(HttpServletRequest request, @PathVariable("anime") String anime) {
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);

    AnimeInfoDTO animeInfo = this.lfAnimeService.animeInfo(anime);

    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{anime}/{chapter}")
  public ResponseEntity<?> chapter(HttpServletRequest request,
      @PathVariable("anime") String anime, @PathVariable("chapter") String chapter) {
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);
    DataUtils.verifySQLInjection(chapter);
    
    if (chapter.contains("-")) {
      Integer chapterNumberPart = Integer.parseInt(chapter.split("-")[0]);
      Integer chapterSecondPart = Integer.parseInt(chapter.split("-")[1]);

      if (chapterNumberPart < 0 || chapterSecondPart != 2 && chapterSecondPart != 5) {
        return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.BAD_REQUEST);
      }
    };

    if (!chapter.contains("-") && Integer.parseInt(chapter) < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.BAD_REQUEST);
    }

    ChapterDTO chapterInfo = this.chapterService.constructChapter(anime, chapter);

    if (DataUtils.isNotNullOrEmpty(chapterInfo)) {
      return new ResponseEntity<>(chapterInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/search/{anime}/{page}")
  public ResponseEntity<?> searchAnimesWithoutMax(HttpServletRequest request,
      @PathVariable("anime") String anime, @PathVariable("page") Integer page) {
    return new ResponseEntity<>(this.searchAnimes(request, anime, page, null), HttpStatus.OK);
  }

  @GetMapping("/search/{anime}/{page}/{maxItems}")
  public ResponseEntity<?> searchAnimesWithMax(HttpServletRequest request,
      @PathVariable("anime") String anime, @PathVariable("page") Integer page,
      @PathVariable("maxItems") Integer maxItems) {
    return new ResponseEntity<>(this.searchAnimes(request, anime, page, maxItems), HttpStatus.OK);
  }

  private SearchDTO searchAnimes(HttpServletRequest request, String anime, Integer page, Integer maxItems) {
    if (page == null || page < 1 || page > 300) {
      throw new InvalidSearch("La página solicitada no es válida.");
    }
    if (maxItems != null && (maxItems < 1 || maxItems > 20)) {
      throw new InvalidSearch("El número de elementos solicitados no es válido.");
    }

    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.verifySQLInjection(anime);

    return this.searchService.searchAnimes(anime, page, maxItems);
  }

}

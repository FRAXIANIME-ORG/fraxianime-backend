package xyz.kiridepapel.fraxianimebackend.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.dto.SearchDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.InvalidSearch;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.ProtectedResource;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.SQLInjectionException;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.service.LfAnimeService;
import xyz.kiridepapel.fraxianimebackend.service.HomeService;
import xyz.kiridepapel.fraxianimebackend.service.LfSearchService;
import xyz.kiridepapel.fraxianimebackend.service.LfChapterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1")
@CrossOrigin(
  origins = { "https://fraxianime.vercel.app", "http://localhost:4200" },
  allowedHeaders = "**"
)
public class JKAnimeController {
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
  private LfAnimeService animeService;
  @Autowired
  private LfChapterService chapterService;
  @Autowired
  private LfSearchService searchService;

  @PostConstruct
  public void init() {
    this.allowedOrigins = Arrays.asList(frontendUrl);
  }

  @GetMapping("/test")
  public ResponseEntity<?> test(
      @RequestParam("date") String lastChapterDate) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
      LocalDate today = LocalDate.now();

      LocalDate date = LocalDate.parse(lastChapterDate, formatter);
      DayOfWeek weekDay = date.getDayOfWeek();

      int daysToAdd = weekDay.getValue() - today.getDayOfWeek().getValue();
      if (daysToAdd != 0 || !date.isBefore(today)) {
        daysToAdd += 7;
      }

      String newDate = today.plusDays(daysToAdd).format(formatter);

      return new ResponseEntity<>(newDate, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/test2")
  public ResponseEntity<?> test2() {
    // Fecha exacta con tiempo
    Date today = new Date();
    // restar 5 horas
    today.setTime(today.getTime() - 18000000);

    return new ResponseEntity<>(today, HttpStatus.OK);
  }
  

  @GetMapping("/animes")
  public ResponseEntity<?> homePage(HttpServletRequest request) {
    this.verifyAllowedOrigin(request.getHeader("Origin"));

    HomePageDTO animes = this.homePageService.homePage();

    if (isNotNullOrEmpty(animes.getSliderAnimes()) &&
        isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
        isNotNullOrEmpty(animes.getAnimesProgramming()) &&
        isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
        isNotNullOrEmpty(animes.getTopAnimes()) &&
        isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
        isNotNullOrEmpty(animes.getLatestAddedList())) {
      return new ResponseEntity<>(animes, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new ResponseDTO("Error al recuperar los datos", 404), HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/{anime}")
  public ResponseEntity<?> animeInfo(HttpServletRequest request,
      @PathVariable("anime") String anime) {

    this.verifyAllowedOrigin(request.getHeader("Origin"));
    this.verifySQLInjection(anime);

    AnimeInfoDTO animeInfo = this.animeService.animeInfo(anime);

    if (isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{anime}/{chapter}")
  public ResponseEntity<?> chapter(HttpServletRequest request,
      @PathVariable("anime") String anime,
      @PathVariable("chapter") Integer chapter) {

    this.verifyAllowedOrigin(request.getHeader("Origin"));
    this.verifySQLInjection(anime);

    if (chapter < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.OK);
    }
    
    ChapterDTO chapterInfo = this.chapterService.constructChapter(anime, chapter);

    if (isNotNullOrEmpty(chapterInfo)) {
      return new ResponseEntity<>(chapterInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/search/{anime}/{page}")
  public ResponseEntity<?> searchAnimesWithoutMax(HttpServletRequest request,
      @PathVariable("anime") String anime,
      @PathVariable("page") Integer page) {

    return new ResponseEntity<>(this.searchAnimes(request, anime, page, null), HttpStatus.OK);
  }

  @GetMapping("/search/{anime}/{page}/{maxItems}")
  public ResponseEntity<?> searchAnimesWithMax(HttpServletRequest request,
      @PathVariable("anime") String anime,
      @PathVariable("page") Integer page,
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

    this.verifyAllowedOrigin(request.getHeader("Origin"));
    this.verifySQLInjection(anime);

    return this.searchService.searchAnimes(anime, page, maxItems);
  }


  private void verifyAllowedOrigin(String origin) {
    if (origin == null || !allowedOrigins.contains(origin)) {
      throw new ProtectedResource("Acceso denegado");
    }
  }

  private void verifySQLInjection(String str) {
    if (str.matches(".*(--|[;+*^$|?{}\\[\\]()'\"\\']).*") || str.contains("SELECT")) {
      throw new SQLInjectionException("Esas cosas son del diablo.");
    }
  }

  private boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  public boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  public boolean isNotNullOrEmpty(Object obj) {
    return obj != null;
  }

}

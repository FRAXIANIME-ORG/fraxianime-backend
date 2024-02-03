package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.Arrays;
import java.util.List;

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
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.SQLInjectionException;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.service.AnimeAnimeLifeService;
import xyz.kiridepapel.fraxianimebackend.service.HomePageService;
import xyz.kiridepapel.fraxianimebackend.service.SearchService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.service.ChapterAnimeLifeService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = { "https://fraxianime.vercel.app", "http://localhost:4200" }, allowedHeaders = "**")
public class JKAnimeController {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeAnimeLifeService animeService;
  @Autowired
  private ChapterAnimeLifeService chapterService;
  @Autowired
  private SearchService searchService;

  private List<String> allowedOrigins;

  @PostConstruct
  public void init() {
    this.allowedOrigins = Arrays.asList(frontendUrl);
  }

  @GetMapping("/test")
  public ResponseEntity<?> test() {
    try {
      // try {
      // Document document = Jsoup.connect("https://jkanime.org").get();
      // log.info("document: " + document);
      // } catch (Exception e) {
      // log.info("error: " + e.getMessage());
      // }
      return new ResponseEntity<>("Ok", HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/animes")
  public ResponseEntity<?> homePage(HttpServletRequest request) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    HomePageDTO animes = this.homePageService.homePage();

    if (DataUtils.isNotNullOrEmpty(animes.getSliderAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
        DataUtils.isNotNullOrEmpty(animes.getAnimesProgramming()) &&
        DataUtils.isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
        DataUtils.isNotNullOrEmpty(animes.getTopAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
        DataUtils.isNotNullOrEmpty(animes.getLatestAddedList())) {
      return new ResponseEntity<>(animes, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(new ResponseDTO("Error al recuperar los datos", 404), HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/{anime}")
  public ResponseEntity<?> animeInfo(HttpServletRequest request, @PathVariable("anime") String anime) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    this.isSQLInjection(anime);

    AnimeInfoDTO animeInfo = this.animeService.animeInfo(anime);

    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{anime}/{chapter}")
  public ResponseEntity<?> chapter(HttpServletRequest request, @PathVariable("anime") String anime,
      @PathVariable("chapter") Integer chapter) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    this.isSQLInjection(anime);

    if (chapter < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.OK);
    }

    ChapterDTO chapterInfo = this.chapterService.constructChapter(anime, chapter);

    if (DataUtils.isNotNullOrEmpty(chapterInfo)) {
      return new ResponseEntity<>(chapterInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/search/{anime}")
  public ResponseEntity<?> searchAnimes(HttpServletRequest request, @PathVariable("anime") String anime) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    this.isSQLInjection(anime);

    return new ResponseEntity<>(this.searchAnimes(anime, 1, null), HttpStatus.OK);
  }

  @GetMapping("/search/{anime}/{page}")
  public ResponseEntity<?> searchAnimesWithoutMax(HttpServletRequest request, @PathVariable("anime") String anime,
      @PathVariable("page") Integer page) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    this.isSQLInjection(anime);

    return new ResponseEntity<>(this.searchAnimes(anime, page, null), HttpStatus.OK);
  }

  @GetMapping("/search/{anime}/{page}/{maxItems}")
  public ResponseEntity<?> searchAnimesWithMax(HttpServletRequest request, @PathVariable("anime") String anime,
      @PathVariable("page") Integer page,
      @PathVariable("maxItems") Integer maxItems) {
    String originHeader = request.getHeader("Origin");

    if (!isOriginAllowed(originHeader)) {
      return new ResponseEntity<>(new ResponseDTO("Origen no permitido", 403), HttpStatus.FORBIDDEN);
    }

    this.isSQLInjection(anime);

    return new ResponseEntity<>(this.searchAnimes(anime, page, maxItems), HttpStatus.OK);
  }

  private boolean isOriginAllowed(String origin) {
    return origin != null && allowedOrigins.contains(origin);
  }

  private void isSQLInjection(String anime) {
    if (DataUtils.isSQLInjection(anime) || anime.contains("SELECT")) {
      throw new SQLInjectionException("Esas cosas son del diablo.");
    }
  }

  private SearchDTO searchAnimes(String anime, Integer page, Integer maxItems) {
    if (page < 1 || page > 100 || page == null) {
      throw new InvalidSearch("La página solicitada no es válida.");
    }
    return this.searchService.searchAnimes(anime, page, maxItems);
  }

}

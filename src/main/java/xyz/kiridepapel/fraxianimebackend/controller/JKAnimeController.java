package xyz.kiridepapel.fraxianimebackend.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.service.AnimeService;
import xyz.kiridepapel.fraxianimebackend.service.ZMethods;
import xyz.kiridepapel.fraxianimebackend.service.HomePageService;
// import xyz.kiridepapel.fraxianimebackend.service.ChapterAnimeFlvService;
import xyz.kiridepapel.fraxianimebackend.service.ChapterAnimeLifeService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = { "https://fraxianime.vercel.app", "http://localhost:4200" }, allowedHeaders = "**")
public class JKAnimeController {
  @Value("${PROVEEDOR_JKANIME_URL}")
  private String proveedorJkAnimeUrl;
  @Value("${PROVEEDOR_ANIMELIFE_URL}")
  private String proveedorAnimeLifeUrl;
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeService animeService;
  // @Autowired
  // private ChapterAnimeFlvService chapterService;
  @Autowired
  private ChapterAnimeLifeService chapterService;

  @GetMapping("/test")
  public ResponseEntity<?> test() {
    try {
      Document document = Jsoup.connect("https://jkanime.org").get();
      // Element element = document.body().select(".CpCnA").first();
      // element = element.select(".contenido").first();
      // Element element = document.body().select(".breadcrumb-option").first();
      // Element element = document.body().select(".contenido").first();
      // Elements elements  = document.select(".breadcrumb-option");

      // log.info("3331362d31303931: " + chapterService.decodeBase64("PGlmcmFtZSB3aWR0aD0iNjQwIiBoZWlnaHQ9IjM2MCIgc3JjPSJodHRwczovL3Nob3J0Lmluay9FZTY3V05ROFEiIGZyYW1lYm9yZGVyPSIwIiBzY3JvbGxpbmc9IjAiIGFsbG93ZnVsbHNjcmVlbj48L2lmcmFtZT4="));

      return new ResponseEntity<>("document: " + document, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/animes")
  public ResponseEntity<?> homePage() {
    try {
      Document jkanime = Jsoup.connect(proveedorJkAnimeUrl).get();
      Document animeLife = Jsoup.connect(proveedorAnimeLifeUrl).get();

      HomePageDTO animes = HomePageDTO.builder()
        .sliderAnimes(this.homePageService.sliderAnimes(jkanime))
        .ovasOnasSpecials(this.homePageService.ovasOnasSpecials(jkanime))
        .animesProgramming(this.homePageService.genericProgramming(jkanime, animeLife, 'a'))
        .donghuasProgramming(this.homePageService.genericProgramming(jkanime, animeLife, 'd'))
        .topAnimes(this.homePageService.topAnimes(jkanime))
        .latestAddedAnimes(this.homePageService.latestAddedAnimes(jkanime))
        .latestAddedList(this.homePageService.latestAddedList(jkanime))
        .build();
      
      if (
        ZMethods.isNotNullOrEmpty(animes.getSliderAnimes()) &&
        ZMethods.isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
        ZMethods.isNotNullOrEmpty(animes.getAnimesProgramming()) &&
        ZMethods.isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
        ZMethods.isNotNullOrEmpty(animes.getTopAnimes()) &&
        ZMethods.isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
        ZMethods.isNotNullOrEmpty(animes.getLatestAddedList())
      ) {
        return new ResponseEntity<>(animes, HttpStatus.OK);
      } else {
        return new ResponseEntity<>("Ocurrió un error al recuperar los datos.", HttpStatus.OK);
      }

    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/{search}")
  public ResponseEntity<?> animeInfo(@PathVariable("search") String search) {
    AnimeInfoDTO animeInfo = this.animeService.getAnimeInfo(search);
    return new ResponseEntity<>(animeInfo, HttpStatus.OK);
  }

  @GetMapping("/{name}/{chapter}")
  public ResponseEntity<?> chapter(
    @PathVariable("name") String name,
    @PathVariable("chapter") String chapter) {
    
    ChapterDTO animeInfo = this.chapterService.chapter(name, chapter);
    
    return new ResponseEntity<>(animeInfo, HttpStatus.OK);
  }

}

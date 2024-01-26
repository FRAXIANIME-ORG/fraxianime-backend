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
import xyz.kiridepapel.fraxianimebackend.service.AnimeAnimeLifeService;
import xyz.kiridepapel.fraxianimebackend.service.HomePageService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.service.ChapterAnimeLifeService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = { "https://fraxianime.vercel.app", "http://localhost:4200" }, allowedHeaders = "**")
public class JKAnimeController {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkAnimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeAnimeLifeService animeService;
  @Autowired
  private ChapterAnimeLifeService chapterService;
  
  @GetMapping("/test")
  public ResponseEntity<?> test() {
    try {
      Document document = Jsoup.connect("https://fraxianime.vercel.app").get();
      // Element element = document.body().select(".contenido").first();

      // AnimeInfoDTO animeInfo = this.animeService.getAnimeInfo(document);

      return new ResponseEntity<>(document, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }
  
  @GetMapping("/animes")
  public ResponseEntity<?> homePage() {
    Document docJkanime = DataUtils.connect(this.providerJkAnimeUrl, "Proveedor 1 inactivo", false);
    Document docAnimelife = DataUtils.connect(this.providerAnimeLifeUrl, "Proveedor 2 inactivo", false);

    HomePageDTO animes = HomePageDTO.builder()
      .sliderAnimes(this.homePageService.sliderAnimes(docJkanime))
      .ovasOnasSpecials(this.homePageService.ovasOnasSpecials(docJkanime))
      .animesProgramming(this.homePageService.animesProgramming(docAnimelife, docJkanime))
      .donghuasProgramming(this.homePageService.donghuasProgramming(docJkanime))
      .topAnimes(this.homePageService.topAnimes(docJkanime))
      .latestAddedAnimes(this.homePageService.latestAddedAnimes(docJkanime))
      .latestAddedList(this.homePageService.latestAddedList(docJkanime))
      .build();
    
    if (
      AnimeUtils.isNotNullOrEmpty(animes.getSliderAnimes()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getAnimesProgramming()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getTopAnimes()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
      AnimeUtils.isNotNullOrEmpty(animes.getLatestAddedList())
    ) {
      return new ResponseEntity<>(animes, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos principales", HttpStatus.OK);
    }
  }

  @GetMapping("/{search}")
  public ResponseEntity<?> animeInfo(@PathVariable("search") String search) {
    Document docJkanime = DataUtils.tryConnectOrReturnNull((this.providerJkAnimeUrl + search), 1);
    Document docAnimelife = DataUtils.tryConnectOrReturnNull((this.providerAnimeLifeUrl + "anime/" + search), 2);

    AnimeInfoDTO animeInfo = this.animeService.getAnimeInfo(docAnimelife, docJkanime, search);

    if (AnimeUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{name}/{chapter}")
  public ResponseEntity<?> chapter(
    @PathVariable("name") String name,
    @PathVariable("chapter") String chapter) {
    
    ChapterDTO animeInfo = this.chapterService.chapter(name, chapter);

    if (AnimeUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.OK);
    }
  }

}

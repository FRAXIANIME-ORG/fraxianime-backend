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
  @Autowired
  private DataUtils dataUtils;
  @Autowired
  AnimeUtils animeUtils;
  
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
    Document docJkanime = this.dataUtils.simpleConnect(this.providerJkAnimeUrl, "Proveedor 1 inactivo");
    Document docAnimelife = this.dataUtils.simpleConnect(this.providerAnimeLifeUrl, "Proveedor 2 inactivo");

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
      DataUtils.isNotNullOrEmpty(animes.getSliderAnimes()) &&
      DataUtils.isNotNullOrEmpty(animes.getOvasOnasSpecials()) &&
      DataUtils.isNotNullOrEmpty(animes.getAnimesProgramming()) &&
      DataUtils.isNotNullOrEmpty(animes.getDonghuasProgramming()) &&
      DataUtils.isNotNullOrEmpty(animes.getTopAnimes()) &&
      DataUtils.isNotNullOrEmpty(animes.getLatestAddedAnimes()) &&
      DataUtils.isNotNullOrEmpty(animes.getLatestAddedList())
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

    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{name}/{chapter}")
  public ResponseEntity<?> chapter(
    @PathVariable("name") String name,
    @PathVariable("chapter") Integer chapter) {

    if (chapter < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.OK);
    }

    String urlChapter = this.providerAnimeLifeUrl + this.animeUtils.specialNameOrUrlCases(name, 's');
    urlChapter = this.animeUtils.specialChapterCases(urlChapter, name, chapter);

    Document docAnimeLife = this.dataUtils.chapterSearchConnect(urlChapter, chapter, "No se encontró el capitulo solicitado.");
    
    ChapterDTO animeInfo = this.chapterService.chapter(docAnimeLife, name, chapter);

    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.OK);
    }
  }

}

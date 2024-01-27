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
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeAnimeLifeService animeService;
  @Autowired
  private ChapterAnimeLifeService chapterService;
  @Autowired
  AnimeUtils animeUtils;
  
  @GetMapping("/test")
  public ResponseEntity<?> test() {
    try {
      Document document = Jsoup.connect("https://fraxianime.vercel.app").get();
      return new ResponseEntity<>(document, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }
  
  @GetMapping("/animes")
  public ResponseEntity<?> homePage() {
    HomePageDTO animes = this.homePageService.homePage();
    
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
    AnimeInfoDTO animeInfo = this.animeService.animeInfo(search);

    if (DataUtils.isNotNullOrEmpty(animeInfo)) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del ánime solicitado.", HttpStatus.OK);
    }
  }

  @GetMapping("/{chapterUrl}/{chapter}")
  public ResponseEntity<?> chapter(
    @PathVariable("chapterUrl") String chapterUrl,
    @PathVariable("chapter") Integer chapter) {

    if (chapter < 0) {
      return new ResponseEntity<>("El capítulo solicitado no es válido.", HttpStatus.OK);
    }
    
    ChapterDTO chapterInfo = this.chapterService.constructChapter(chapterUrl, chapter);

    if (DataUtils.isNotNullOrEmpty(chapterInfo)) {
      return new ResponseEntity<>(chapterInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("Ocurrió un error al recuperar los datos del capítulo solicitado.", HttpStatus.OK);
    }
  }

}

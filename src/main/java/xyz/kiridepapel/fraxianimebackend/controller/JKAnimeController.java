package xyz.kiridepapel.fraxianimebackend.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.SpecificChapterDTO;
import xyz.kiridepapel.fraxianimebackend.service.AnimeInfoService;
import xyz.kiridepapel.fraxianimebackend.service.ZMethods;
import xyz.kiridepapel.fraxianimebackend.service.HomePageService;
import xyz.kiridepapel.fraxianimebackend.service.SpecificChapterService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = { "https://fraxianime.vercel.app", "http://localhost:4200" }, allowedHeaders = "**")
public class JKAnimeController {
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeInfoService animeInfoService;
  @Autowired
  private SpecificChapterService specificChapterService;

  @GetMapping("/page/{page}")
  public ResponseEntity<?> homePage(@PathVariable("page") Integer page) {
    try {
      String pageUrl = "https://animeflv.com.ru/page/" + page + "/";
      Document document = Jsoup.connect(pageUrl).get();

      HomePageDTO animes = HomePageDTO.builder()
        .lastChapters(this.homePageService.lastChapters(document))
        .allAnimes(this.homePageService.allAnimes(document))
        .emisionAnimes(this.homePageService.emisionAnimes(document))
        .build();
      
      if (
        ZMethods.isNotNullOrEmpty(animes.getLastChapters()) &&
        ZMethods.isNotNullOrEmpty(animes.getAllAnimes()) &&
        ZMethods.isNotNullOrEmpty(animes.getEmisionAnimes())
      ) {
        return new ResponseEntity<>(animes, HttpStatus.OK);
      } else {
        return new ResponseEntity<>("No se recuperaron todos los datos.", HttpStatus.OK);
      }

    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/{search}")
  public ResponseEntity<?> animeInfo(@PathVariable("search") String search) {
    AnimeInfoDTO animeInfo = this.animeInfoService.getAnimeInfo(search);
    return new ResponseEntity<>(animeInfo, HttpStatus.OK);
  }

  @GetMapping("/{name}/{chapter}")
  public ResponseEntity<?> specificChapter(
    @PathVariable("name") String name,
    @PathVariable("chapter") String chapter) {
    
    SpecificChapterDTO animeInfo = this.specificChapterService.specificChapter(name, chapter);
    
    if (
      ZMethods.isNotNullOrEmpty(animeInfo.getName()) &&
      ZMethods.isNotNullOrEmpty(animeInfo.getSrcOptions())
    ) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("No se pudo obtener la información del anime.", HttpStatus.OK);
    }
  }

}

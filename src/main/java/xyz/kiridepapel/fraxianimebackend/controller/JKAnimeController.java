package xyz.kiridepapel.fraxianimebackend.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  @Value("${PROVEEDOR_ALL_URL}")
  private String proveedorAllUrl;
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private AnimeInfoService animeInfoService;
  @Autowired
  private SpecificChapterService specificChapterService;

  @GetMapping("/test")
  public ResponseEntity<?> test() {
    try {
      Document document = Jsoup.connect("https://jkanime.org/one-piece/1090").get();
      Element element = document.body();
      element = element.select(".contenido").first();
      // Element element = document.body().select(".breadcrumb-option").first();
      // Element element = document.body().select(".contenido").first();
      // Elements elements  = document.select(".breadcrumb-option");

      return new ResponseEntity<>("asd: " + element, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Ocurrió un error: " + e.getMessage(), HttpStatus.valueOf(500));
    }
  }

  @GetMapping("/animes")
  public ResponseEntity<?> homePage() {
    try {
      Document document = Jsoup.connect(proveedorAllUrl).get();

      HomePageDTO animes = HomePageDTO.builder()
        .sliderAnimes(this.homePageService.sliderAnimes(document))
        .ovasOnasSpecials(this.homePageService.ovasOnasSpecials(document))
        .animesProgramming(this.homePageService.genericProgramming(document, 'a'))
        .donghuasProgramming(this.homePageService.genericProgramming(document, 'd'))
        .topAnimes(this.homePageService.topAnimes(document))
        .latestAddedAnimes(this.homePageService.latestAddedAnimes(document))
        .latestAddedList(this.homePageService.latestAddedList(document))
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

package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.SpecificChapterDTO;
import xyz.kiridepapel.fraxianimebackend.service.JKAnimeService;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = { "https://fraxianime.vercel.app", "http://localhost:4200" }, allowedHeaders = "**")
public class JKAnimeController {
  @Autowired
  private JKAnimeService jKAnimeService;

  @GetMapping("/page/{page}")
  public ResponseEntity<?> homePage(@PathVariable("page") Integer page) {
    try {
      String urlBase = "https://animeflv.com.ru/page/" + page + "/";
      Document document = Jsoup.connect(urlBase).get();

      HomePageDTO animes = HomePageDTO.builder()
        .lastChapters(this.jKAnimeService.lastChapters(document))
        .allAnimes(this.jKAnimeService.allAnimes(document))
        .emisionAnimes(this.jKAnimeService.emisionAnimes(document))
        .build();
      
      if (
        isNotNullOrEmpty(animes.getLastChapters()) &&
        isNotNullOrEmpty(animes.getAllAnimes()) &&
        isNotNullOrEmpty(animes.getEmisionAnimes())
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
    AnimeInfoDTO animeInfo = this.jKAnimeService.getAnimeInfo(search);
    return new ResponseEntity<>(animeInfo, HttpStatus.OK);
  }

  @GetMapping("/{name}/{chapter}")
  public ResponseEntity<?> specificChapter(
    @PathVariable("name") String name,
    @PathVariable("chapter") Integer chapter) {
    
    SpecificChapterDTO animeInfo = this.jKAnimeService.specificChapter(name, chapter);
    
    if (isNotNullOrEmpty(animeInfo.getName()) && isNotNullOrEmpty(animeInfo.getIframeSrc())) {
      return new ResponseEntity<>(animeInfo, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("No se pudo obtener la información del anime.", HttpStatus.OK);
    }
  }

  private boolean isNotNullOrEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  private boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }

}

package xyz.kiridepapel.fraxianimebackend.config;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.dto.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.service.LfChapterService;
import xyz.kiridepapel.fraxianimebackend.service.ScheduleService;
import xyz.kiridepapel.fraxianimebackend.service.HomeService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Component
@Log
public class CacheScheduler {
  @Autowired
  private CacheManager cacheManager;
  @Autowired
  private HomeService homePageService;
  @Autowired
  private LfChapterService chapterService;
  @Autowired
  private DataUtils dataUtils;
  @Autowired
  private ScheduleService scheduleService;
  
  // ! Busca nuevo caché cada: 30 min. = 1800000 ms.
  @Scheduled(fixedRate = 1805000)
  public void updateCache() {
    this.updateCacheProgramming();
  }
  
  public void updateCacheProgramming() {
    // Espera 1 segundo
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Actualiza el caché de los casos especiales
    this.updateSpecialCases();

    // Obtiene o guarda en caché de toda la página de inicio
    HomePageDTO home = this.homePageService.homePage();
    List<ChapterDataDTO> animesProgramming = home.getAnimesProgramming();
    Random random = new Random();
    int counter = 1;
    
    for (ChapterDataDTO chapterInfo : animesProgramming) {
      try {
        boolean isCached = false;
        int chapter = Integer.parseInt(chapterInfo.getUrl().split("/")[1]);
        String url = chapterInfo.getUrl().split("/")[0];
        
        if (url.contains("/")) {
          url = url.split("/")[0];
        }

        // Comprueba si el capítulo ya está en caché
        ChapterDTO chapterCache = this.dataUtils.searchFromCache(cacheManager, ChapterDTO.class, "chapter", (url + "/" + chapter));
        if (chapterCache != null) {
          isCached = true;
        }

        // Si está en caché, pasa al siguiente capítulo
        if (isCached) {
          log.info(String.format("%02d", counter++) + ". Ya esta en cache: '" + url + "' (" + String.format("%02d", chapter) + ")");
          continue;
        } else {
          // Si no está en caché, busca el capítulo en la web y lo guarda en caché
          try {
            // Espera de 3 a 5 segundos antes de buscar el capítulo en la web
            int randomTime = 3000 + (random.nextInt(2001));
            Thread.sleep(randomTime);
            log.info(String.format("%02d", counter++) + ". Guardando en cache: '" + url + "' (" + String.format("%02d", chapter) + ")");
            this.chapterService.cacheChapter(url, chapter);
          } catch (InterruptedException e) {
            log.severe("Error Schedule: " + e.getMessage());
          }
        }
      } catch (Exception e) {
        log.severe("-----------------------------");
        log.severe(String.format("%02d", counter) + ". " + e.getMessage());
        log.severe(String.format("%02d", counter) + ". Name: " + chapterInfo.getName() + " (" + chapterInfo.getChapter() + ") no se pudo guardar en cache.");
        log.severe(String.format("%02d", counter) + ". Url: " + chapterInfo.getUrl());
        log.severe("-----------------------------");
        counter++;
      }
    }
      
    log.info("-----------------------------");
  }

  // Obtiene o guarda en caché de los casos especiales
  private void updateSpecialCases() {
    List<Character> listSpecialCharacters = List.of(
      'h', // Home
      'j', // url: anime - JkAnime
      'y', // url: history - JkAnime
      'a', // url: anime - AnimeLife
      'c', // url: chapter - AnimeLife
      'n', // name: anime, chapter - AnimeLife
      'l', // name: chapter list of chapter - AnimeLife
      's' // url: search - AnimeLife
    );

    // Borra el caché de los casos especiales
    log.info("-----------------------------");
    DataUtils.deleteFromCache(cacheManager, "specialCases", null, true);
    // Guarda en caché los casos especiales actualizados
    log.info("-----------------------------");
    for (Character type : listSpecialCharacters) {
      this.scheduleService.getSpecialCases(type);
      log.info("00. Guardando en cache: '" + type + "'");
    }
    log.info("-----------------------------");
  }

}

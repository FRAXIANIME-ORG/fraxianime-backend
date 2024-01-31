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
import xyz.kiridepapel.fraxianimebackend.service.ChapterAnimeLifeService;
import xyz.kiridepapel.fraxianimebackend.service.HomePageService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;

@Component
@Log
public class CacheScheduler {
  @Autowired
  private HomePageService homePageService;
  @Autowired
  private ChapterAnimeLifeService chapterService;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private AnimeUtils animeUtils;
  
  // ! Busca nuevo caché cada: 30 min. = 1800000 ms.
  @Scheduled(fixedRate = 1805000)
  public void updateCache() {
    this.updateCacheProgramming();
  }
  
  public void updateCacheProgramming() {
    // Obtiene o guarda en caché de toda la página de inicio
    HomePageDTO home = this.homePageService.homePage();
    // Busca en caché cada uno de los últimos capítulos de ánime programados obtenidos de la página de inicio
    List<ChapterDataDTO> animesProgramming = home.getAnimesProgramming();
    Random random = new Random();
    int counter = 1;
    
    for (ChapterDataDTO chapterInfo : animesProgramming) {
      try {
        boolean isCached = false;
        String url = chapterInfo.getUrl().split("/")[0];
        int chapter = Integer.parseInt(chapterInfo.getUrl().split("/")[1]);
        
        // Comprueba si el capítulo ya está en caché
        ChapterDTO chapterCache = this.animeUtils.searchFromCache(cacheManager, "chapter", (url + "/" + chapter), ChapterDTO.class);
        if (chapterCache != null) {
          isCached = true;
        }

        // Si está en caché, pasa al siguiente capítulo
        if (isCached) {
          log.info(String.format("%02d", counter++) + ". Ya esta en cache: " + url + " (" + String.format("%02d", chapter) + ")");
          continue;
        } else {
          // Si no está en caché, busca el capítulo en la web y lo guarda en caché
          try {
            // Espera de 3 a 5 segundos antes de buscar el capítulo en la web
            int randomTime = 3000 + (random.nextInt(2001));
            Thread.sleep(randomTime);
            log.info(String.format("%02d", counter++) + ". Guardando en cache: " + url + " (" + String.format("%02d", chapter) + ")");
            this.chapterService.cacheChapter(url, chapter);
          } catch (InterruptedException e) {
            log.severe("Error: " + e.getMessage());
          }
        }
      } catch (Exception e) {
        log.severe(String.format("%02d", counter++) + ". Error: El capitulo del anime " + chapterInfo.getName() + " no se pudo guardar en cache.");
        log.severe(String.format("%02d", counter) + ". Error exacto: " + e.getMessage());
      }
    }
  }

}

package xyz.kiridepapel.fraxianimebackend.config;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.service.anime.JkLfHomeService;
import xyz.kiridepapel.fraxianimebackend.service.anime.LfChapterService;
import xyz.kiridepapel.fraxianimebackend.service.anime.LfDirectoryService;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;

@Component
@Log
public class CacheScheduler {
  // Variables
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  // Inyección de dependencias
  @Autowired
  private JkLfHomeService homeService;
  @Autowired
  private LfChapterService chapterService;
  @Autowired
  private LfDirectoryService directoryService;
  @Autowired
  private CacheUtils cacheUtils;
  @Autowired
  private CacheManager cacheManager;
  
  // 1. 30 min. + 5 sec. = 1805000 ms
  @Scheduled(fixedRate = 1805000)
  public void updateHome() {
    this.updateCacheProgramming();
  }

  // 2. 7 days + 5 sec. = 604805000 ms
  // 2. Borra y actualiza el caché de las 'Opciones de directorio'
  @Scheduled(fixedRate = 604805000)
  public void updateDirectoryOptions() {
    log.info("-----------------------------");
    CacheUtils.deleteFromCache(cacheManager, "directory", null, true);
    log.info("00. Guardando en cache: 'directory/options'");
    this.directoryService.directoryOptions("options");
    log.info("01. Guardando en cache: 'directory/?page=1'");
    this.directoryService.saveLongDirectoryAnimes("?page=1");
    log.info("-----------------------------");
  }
  
  // 1. Borra y actualiza el caché de la 'Página de inicio'
  public void updateCacheProgramming() {
    // Actualiza el caché de los casos especiales
    this.updateSpecialCases();
    
    // Borra el caché de la página de inicio si está en producción
    if (isProduction == true) {
      log.info("-----------------------------");
      CacheUtils.deleteFromCache(cacheManager, "home", null, true);
      log.info("-----------------------------");
    }

    // Guarda en caché la página de inicio actualizada
    HomePageDTO home = this.homeService.homePage();

    // Guarda en caché los últimos capítulos
    log.info("-----------------------------");
    this.saveLastChapters(home.getAnimesProgramming());
    log.info("-----------------------------");
  }

  // 1. Obtiene o guarda en caché los 'Últimos capítulos emitidos'
  private void saveLastChapters(List<ChapterDataDTO> animesProgramming) {
    // Variables
    Random random = new Random();
    int counter = 1;
    // Guarda en caché los últimos capítulos
    for (ChapterDataDTO chapterInfo : animesProgramming) {
      try {
        boolean isCached = false;
        String chapter = chapterInfo.getUrl().split("/")[1];
        String url = chapterInfo.getUrl().split("/")[0];
        
        if (url.contains("/")) {
          url = url.split("/")[0];
        }

        // * Comprueba si el capítulo ya está en caché
        ChapterDTO chapterCache = CacheUtils.searchFromCache(cacheManager, ChapterDTO.class, "chapter", (url + "/" + chapter));
        if (chapterCache != null) {
          isCached = true;
        }

        // * Da formato al capitulo si es necesario (X -> 0X)
        String chapterFormatted;
        if (!chapter.contains("-")) {
          chapterFormatted = String.format("%02d", Integer.parseInt(chapter));
        } else {
          chapterFormatted = chapter;
        }

        // * Si el capítulo no está en caché, lo guarda
        if (isCached) {
          log.info(String.format("%02d", counter++) + ". Ya esta en cache: '" + url + "' (" + chapterFormatted + ")");
          continue;
        } else {
          try {
            // Espera de 3 a 5 segundos antes de buscar el capítulo en la web
            int randomTime = 3000 + (random.nextInt(2001));
            Thread.sleep(randomTime);

            // Guarda en caché el capítulo
            log.info(String.format("%02d", counter++) + ". Guardando en cache: '" + url + "' (" + chapterFormatted + ")");
            this.chapterService.saveLongCacheChapter(url, chapter);
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
  }

  // 1. Obtiene o guarda en caché de los casos especiales
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
    CacheUtils.deleteFromCache(cacheManager, "specialCases", null, true);
    // Guarda en caché los casos especiales actualizados
    int counter = 0;
    for (Character type : listSpecialCharacters) {
      this.cacheUtils.getSpecialCases(type);
      log.info(String.format("%02d", counter) + ". Guardando en cache: '" + type + "'");
      counter++;
    }
    log.info("-----------------------------");
  }
}

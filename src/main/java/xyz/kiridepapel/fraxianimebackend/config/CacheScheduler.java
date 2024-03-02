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
import xyz.kiridepapel.fraxianimebackend.service.anime.JkScheduleService;
import xyz.kiridepapel.fraxianimebackend.service.anime.JkTopService;
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
  private JkScheduleService scheduleService;
  @Autowired
  private JkTopService topService;
  @Autowired
  private CacheUtils cacheUtils;
  @Autowired
  private CacheManager cacheManager;
  
  // * PROGRAMACIÓN DE TAREAS
  // 1. 30 minutos: 'Home'
  @Scheduled(fixedRate = 1805000)
  public void autoUpdateHome() {
    this.updateHome();
  }
  // 2. 7 días: 'Directorio'
  @Scheduled(fixedRate = 604805000)
  public void autoUpdateDirectory() {
    this.updateDirectory();
  }
  // 3. 1 hora: 'Horario'
  @Scheduled(fixedRate = 3605000)
  public void autoUpdateSchedule() {
    this.updateSchedule();
  }
  // 4. 7 días: 'Top'
  @Scheduled(fixedRate = 604805000)
  public void autoUpdateTop() {
    this.updateTop();
  }
  
  // * MÉTODOS INTERMEDIOS
  // 1. Fuerza la actualizació de: 'Home' (30 min.), 'Casos Especiales' (30 min.) y 'Últimos capítulos emitidos' (7 días)
  private void updateHome() {
    // Casos especiales
    log.info("-----------------------------------------------------");
    this.updateSpecialCases();
    log.info("-----------------------------------------------------");
    // Home
    log.info("-----------------------------------------------------");
    CacheUtils.deleteFromCache(cacheManager, "home", null, true);
    log.info("Guardando en cache: 'home'");
    HomePageDTO home = this.homeService.home();
    log.info("-----------------------------------------------------");
    // Últimos capítulos emitidos
    log.info("-----------------------------------------------------");
    this.getOrSaveLastChapters(home.getAnimesProgramming());
    log.info("-----------------------------------------------------");
  }
  // 2. Fuerza la actualización de 'Directorio'
  private void updateDirectory() {
    log.info("-----------------------------------------------------");
    CacheUtils.deleteFromCache(cacheManager, "directory", null, true);
    log.info("Guardando en cache: 'directory/options'");
    this.directoryService.directoryOptions("options");
    log.info("Guardando en cache: 'directory/?page=1'");
    this.directoryService.saveLongDirectoryAnimes("?page=1");
    log.info("-----------------------------------------------------");
  }
  // 3. Fuerza la actualización de 'Horario'
  private void updateSchedule() {
    log.info("-----------------------------------------------------");
    CacheUtils.deleteFromCache(cacheManager, "schedule", null, true);
    log.info("Guardando en cache: 'schedule'");
    this.scheduleService.getSchedule("list");
    log.info("-----------------------------------------------------");
  }
  // 4. Fuerza la actualización de 'Top'
  private void updateTop() {
    log.info("-----------------------------------------------------");
    CacheUtils.deleteFromCache(cacheManager, "top", null, true);
    log.info("Guardando en cache: 'top'");
    this.topService.getTop("list");
    log.info("-----------------------------------------------------");
  }

  // * MÉTODOS DE TAREAS
  // Obtiene o guarda en caché los 'Últimos capítulos emitidos'
  private void getOrSaveLastChapters(List<ChapterDataDTO> animesProgramming) {
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
        log.info("-----------------------------------------------------");
        log.severe(String.format("%02d", counter) + ". " + e.getMessage());
        log.severe(String.format("%02d", counter) + ". Name: " + chapterInfo.getName() + " (" + chapterInfo.getChapter() + ") no se pudo guardar en cache.");
        log.severe(String.format("%02d", counter) + ". Url: " + chapterInfo.getUrl());
        log.info("-----------------------------------------------------");
        counter++;
      }
    }
  }
  // Fuerza la actualización de los casos especiales
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
    CacheUtils.deleteFromCache(cacheManager, "specialCases", null, true);
    // Guarda en caché los casos especiales actualizados
    int counter = 0;
    for (Character type : listSpecialCharacters) {
      this.cacheUtils.getSpecialCases(type);
      log.info(String.format("%02d", counter) + ". Guardando en cache: '" + type + "'");
      counter++;
    }
  }
}

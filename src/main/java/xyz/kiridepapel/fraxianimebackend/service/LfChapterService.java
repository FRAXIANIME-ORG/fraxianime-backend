package xyz.kiridepapel.fraxianimebackend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service

@Log
public class LfChapterService {
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private CacheManager cacheManager;
  @Autowired
  private AnimeUtils animeUtils;
  @Autowired
  private DataUtils dataUtils;

  // Este método lo usa el sistema para guardar la información de los últimos 9 capítulos salidos automáticamente
  @Cacheable(value = "chapter", key = "#url.concat('/').concat(#chapter)")
  public ChapterDTO cacheChapter(String url, String chapter) {
    return this.constructChapter(url, chapter);
  }

  // Obtener la información de un capítulo, desde caché si existe o desde la web si no existe
  public ChapterDTO constructChapter(String url, String chapter) {
    try {
      ChapterDTO chapterCache = this.dataUtils.searchFromCache(cacheManager, ChapterDTO.class, "chapter", (url + "/" + chapter));
      if (chapterCache != null) {
        return chapterCache;
      }

      // Lógica para obtener el ChapterDTO si no está en caché (no se almacena en caché)
      String modifiedUrlChapter = this.providerAnimeLifeUrl + this.animeUtils.specialNameOrUrlCases(null, url, 'c');
      modifiedUrlChapter = this.animeUtils.specialChapterCases(modifiedUrlChapter, url, chapter);
      return this.findChapter(modifiedUrlChapter, chapter);
    } catch (Exception e) {
      log.info("Error en constructChapter: " + e.getMessage());
      throw new ChapterNotFound("Error construyendo el capítulo");
    }
  }

  public ChapterDTO findChapter(String modifiedUrlChapter, String chapter) {
    try {
      Document docAnimeLife = this.animeUtils.chapterSearchConnect(modifiedUrlChapter, chapter, "No se encontró el capitulo solicitado.");
      
      // Obtiene los capítulos cercanos para determinar si hay capítulos anteriores o siguientes
      Elements nearChapters = docAnimeLife.body().select(".naveps .nvs");
      
      String name = docAnimeLife.select(".ts-breadcrumb li").get(1).select("span").text().trim();
      ChapterDTO chapterInfo = ChapterDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(null, name, 'n'))
        .srcOptions(this.getSrcOptions(docAnimeLife))
        .downloadOptions(this.getDownloadOptions(docAnimeLife))
        .havePreviousChapter(this.havePreviousChapter(nearChapters))
        .haveNextChapter(this.haveNextChapter(nearChapters))
        .build();
      
      // Si no tiene siguiente capítulo, se obtiene la fecha de emisión del último capítulo y se le suma 7 días
      if (!chapterInfo.getHaveNextChapter()) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
        String date = docAnimeLife.body().select(".year .updated").text().trim();
        date = DataUtils.parseDate(date, formatter, 0);
        chapterInfo.setNextChapterDate(this.calcNextChapterDate(date));
      }

      // Establece el estado del ánime (en emisión o completado)
      String state = docAnimeLife.body().select(".det").first().select("span i").text().trim();
      if (state.equals("Completada")) {
        chapterInfo.setInEmision(false);
      } else if (state.equals("En emisión")) {
        chapterInfo.setInEmision(true);
      }
      
      // Establece la información de los capítulos cercanos al actual (anterior y siguiente)
      chapterInfo = this.setPrevAndNextChapters(chapterInfo, docAnimeLife, nearChapters, chapter);

      return chapterInfo;
    } catch (Exception e) {
      log.info("Error en findChapter: " + e.getMessage());
      throw new ChapterNotFound("Error buscando el capítulo");
    }
  }

  private List<LinkDTO> getSrcOptions(Document docAnimeLife) {
    try {
      List<LinkDTO> list = new ArrayList<>();
      Elements srcs = docAnimeLife.body().select(".mirror option");

      srcs.remove(0); // Elimina el primer elemento: "Seleccionar servidor"

      for (Element src : srcs) {
        String url = DataUtils.decodeBase64(src.attr("value"), true);

        if (url.startsWith("//")) {
          url = "https://" + url.substring(2);
        }

        LinkDTO link = new LinkDTO();
        link.setName(src.text().trim());
        link.setUrl(url);
        
        list.add(link);
      }
  
      // Ordena los servidores de reproducción
      Comparator<LinkDTO> comparator = (src1, src2) -> {
        List<String> order = Arrays.asList("Voe", "YourUpload", "FileMoon", "VidGuard");
        int index1 = order.indexOf(src1.getName());
        int index2 = order.indexOf(src2.getName());

        if (index1 == -1) index1 = Integer.MAX_VALUE;
        if (index2 == -1) index2 = Integer.MAX_VALUE;

        return index1 - index2;
      };
      Collections.sort(list, comparator);
      
      // Elimita los servidores de reproducción malogrados
      list.removeIf(src -> src.getUrl().contains("<center>"));
  
      return list;
    } catch (Exception e) {
      throw new ChapterNotFound("2: " + e.getMessage());
    }
  }

  private List<LinkDTO> getDownloadOptions(Document docAnimeLife) {
    try {
      Element element = docAnimeLife.body().select(".iconx").first().select("a").first();
      if (element != null) {
        List<LinkDTO> list = new ArrayList<>();
    
        LinkDTO link = LinkDTO.builder()
          .name(this.getProviderName(element.attr("href")))
          .url(element.attr("href"))
          .build();
    
        list.add(link);
    
        return list;
      } else {
        Elements elements = docAnimeLife.body().select(".bixbox .soraurlx a");
        if (elements != null) {
          List<LinkDTO> list = new ArrayList<>();
    
          for (Element element2 : elements) {
            LinkDTO link = LinkDTO.builder()
              .name(this.getProviderName(element2.attr("href")))
              .url(element2.attr("href"))
              .build();
            
            list.add(link);
          }
    
          return list;
        } else {
          return null;
        }
      }
    } catch (Exception e) {
      throw new ChapterNotFound("3: " + e.getMessage());
    }
  }

  private ChapterDTO setPrevAndNextChapters(ChapterDTO chapterInfo, Document docAnimeLife, Elements nearChapters, String chapter) {
    try {
      Elements episodeList = docAnimeLife.body().select(".episodelist ul li");

      // Variables de los capitulos
      String actualChapter = chapter.replace("-", ".");
      Element itemFirstChapter = episodeList.last();
      Element itemLastChapter = episodeList.first();

      if (itemFirstChapter != null && itemLastChapter != null) {
        // Imagen del capítulo
        String chapterImg = itemLastChapter.select(".thumbnel img").attr("src").replace("?resize=130,130", "");
        // El nombre del ánime puede estar modificado en casos especiales
        String chapterNameModified = this.animeUtils.specialNameOrUrlCases(null, chapterInfo.getName(), 'l');

        // Obtiene el numero del capitulo anterior si es que existe
        Pattern patternNumber = Pattern.compile("-(\\d{1,2})(?:-(\\d+))?/?$");
        if (chapterInfo.getHavePreviousChapter()) {
          String previousChapterUrl = nearChapters.first().select("a").attr("href").replace(providerAnimeLifeUrl, "");
          Matcher matcherNumber = patternNumber.matcher(previousChapterUrl);

          String chapterNumber = "";
          if (matcherNumber.find()) {
            // Si el capitulo es name-anime-12-2, se obtiene 12-2
            if (matcherNumber.group(2) != null) {
              String chapterGroup = matcherNumber.group(1) + "-" + matcherNumber.group(2);
              // Si el capitulo es 12-2, se obtiene 11, si es 12-5, se queda así.
              if (chapterGroup.contains("-2")) {
                chapterNumber = String.valueOf(Integer.parseInt(matcherNumber.group(1)) - 1);
              } else {
                chapterNumber = chapterGroup;
              }
            } else {
              chapterNumber = String.valueOf(Integer.parseInt(matcherNumber.group(1)));
            }
          }

          chapterInfo.setPreviousChapter(chapterNumber);
        }
        
        // Obtiene el numero del capitulo siguiente si es que existe
        if (chapterInfo.getHaveNextChapter()) {
          String nextChapterUrl = nearChapters.last().select("a").attr("href").replace(providerAnimeLifeUrl, "");
          Matcher matcherNumber = patternNumber.matcher(nextChapterUrl);

          String chapterNumber = "";
          if (matcherNumber.find()) {
            // Si el capitulo es name-anime-12-2, se obtiene 12-2
            if (matcherNumber.group(2) != null) {
              String chapterGroup = matcherNumber.group(1) + "-" + matcherNumber.group(2);
              // Si el capitulo es 12-2, se obtiene 13, si es 12-5, se queda así.
              if (chapterGroup.contains("-2")) {
                chapterNumber = String.valueOf(Integer.parseInt(matcherNumber.group(1)) + 1);
              } else {
                chapterNumber = chapterGroup;
              }
            } else {
              chapterNumber = String.valueOf(Integer.parseInt(matcherNumber.group(1)));
            }
          }

          chapterInfo.setNextChapter(chapterNumber);
        }
        
        // Obtiene el numero del primer y último capitulo
        String firstChapter = this.getChapterFromName(itemFirstChapter, chapterNameModified);
        String lastChapter = this.getChapterFromName(itemLastChapter, chapterNameModified);
        // Si el capítulo está en emisión, no se establece el último capítulo
        chapterInfo.setFirstChapter(firstChapter);
        if (!chapterInfo.getInEmision()) chapterInfo.setLastChapter(lastChapter);

        // Fecha del último capítulo
        String lastChapterDate = itemLastChapter.select(".playinfo span").text();
        String[] chapterDateArray = lastChapterDate.split(" - ");
        lastChapterDate = chapterDateArray[chapterDateArray.length - 1];

        // Asigna la información
        chapterInfo.setChapterImg(chapterImg);
        chapterInfo.setActualChapter(actualChapter);
        chapterInfo.setLastChapterDate(lastChapterDate);
      }

      return chapterInfo;
    } catch (Exception e) {
      log.severe("Error en setPrevAndNextChapters: " + e.getMessage());
      throw new ChapterNotFound("Error estableciendo los capítulos");
    }
  }

  public String getChapterFromName(Element item, String chapterNameModified) {
    String number = item.select(".playinfo h4").text().replace(chapterNameModified, "").trim();

    // Elimina los 0s a la izquierda si es que hay
    if (!number.contains(".")) {
      number = String.valueOf(Integer.parseInt(number));
    }

    return number;
  }

  public String calcNextChapterDate(String lastChapterDate) {
    if (lastChapterDate == null || lastChapterDate.isEmpty()) {
      return null;
    }
    
    DateTimeFormatter formatterIn = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    DateTimeFormatter formatterOut = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    
    LocalDate todayLDT = DataUtils.getLocalDateTimeNow(this.isProduction).toLocalDate();
    
    LocalDate date = LocalDate.parse(lastChapterDate, formatterIn);
    DayOfWeek weekDay = date.getDayOfWeek();
    
    int daysToAdd = weekDay.getValue() - todayLDT.getDayOfWeek().getValue();
    
    // Si el capitulo sera emitido en los proximos dias, solo se suma la cantidad de dias
    // que hay desde hoy hasta el dia de la semana en el que salio el ultimo capitulo
    //
    // Si el capitulo fue emitido antes de hoy, se suma 7 dias al dia de la semana de la fecha recibida
    if (daysToAdd < 0) daysToAdd += 7;
    // Si el capitulo fue emitido hoy, se suma 7 dias al dia de la semana de la fecha recibida
    if (daysToAdd == 0 && date.isEqual(todayLDT)) daysToAdd += 7;

    // Se suman la cantidad de dias que hay desde hoy hasta
    // el dia de la semana en el que salio el ultimo capitulo tomando en cuenta las condiciones anteriores
    String finalDate = todayLDT.plusDays(daysToAdd).format(formatterOut);

    return finalDate;
  }

  private String getProviderName(String url) {
    String regex = "https://(?:www\\.)?([^\\.]+)";
    Pattern pattern = java.util.regex.Pattern.compile(regex);
    Matcher matcher = pattern.matcher(url);
    
    if (matcher.find()) {
      String provider = matcher.group(1);
      return provider.substring(0, 1).toUpperCase() + provider.substring(1);
    } else {
      return "Privado";
    }
  }

  private boolean havePreviousChapter(Elements nearChapters) {
    Element previousChapter = nearChapters.first().select("a").first();
    return previousChapter != null ? true : false;
  }

  private boolean haveNextChapter(Elements nearChapters) {
    Element nextChapter = nearChapters.last().select("a").first();
    return nextChapter != null ? true : false;
  }
  
}

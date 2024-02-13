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

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
public class LfChapterService {
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
  public ChapterDTO cacheChapter(String url, Integer chapter) {
    return this.constructChapter(url, chapter);
  }

  // Obtener la información de un capítulo, desde caché si existe o desde la web si no existe
  public ChapterDTO constructChapter(String url, Integer chapter) {
    ChapterDTO chapterCache = this.animeUtils.searchFromCache(cacheManager, "chapter", (url + "/" + chapter), ChapterDTO.class);
    if (chapterCache != null) {
      return chapterCache;
    }

    // Lógica para obtener el ChapterDTO si no está en caché (no se almacena en caché)
    String modifiedUrlChapter = this.providerAnimeLifeUrl + this.animeUtils.specialNameOrUrlCases(url, 'c');
    modifiedUrlChapter = this.animeUtils.specialChapterCases(modifiedUrlChapter, url, chapter);
    return this.findChapter(modifiedUrlChapter, chapter);
  }

  public ChapterDTO findChapter(String modifiedUrlChapter, Integer chapter) {
    try {
      Document docAnimeLife = this.dataUtils.chapterSearchConnect(
          modifiedUrlChapter, chapter,
          "No se encontró el capitulo solicitado.");
      // Obtiene los capítulos cercanos para determinar si hay capítulos anteriores o siguientes
      Elements nearChapters = docAnimeLife.body().select(".naveps .nvs");

      ChapterDTO chapterInfo = ChapterDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(docAnimeLife.select(".ts-breadcrumb li").get(1).select("span").text().trim(), 'n'))
        .srcOptions(this.getSrcOptions(docAnimeLife))
        .downloadOptions(this.getDownloadOptions(docAnimeLife))
        .havePreviousChapter(this.havePreviousChapter(nearChapters))
        .haveNextChapter(this.haveNextChapter(nearChapters))
        .build();
      
      // Si no tiene siguiente capítulo, se obtiene la fecha de emisión del último capítulo y se le suma 7 días
      if (!chapterInfo.getHaveNextChapter()) {
        String date = DataUtils.parseDate(docAnimeLife.body().select(".year .updated").text().trim(), "MMMM d, yyyy", 7);
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
      chapterInfo = this.setFirstAndLastChapters(chapterInfo, docAnimeLife, chapter);

      return chapterInfo;
    } catch (Exception e) {
      throw new ChapterNotFound(e.getMessage());
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
        List<String> order = Arrays.asList("Voe", "YourUpload", "VidGuard");
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

  private String calcNextChapterDate(String lastChapterDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    
    LocalDate today = this.dataUtils.getDateNow().toLocalDate();
    
    LocalDate date = LocalDate.parse(lastChapterDate, formatter);
    DayOfWeek weekDay = date.getDayOfWeek();

    int daysToAdd = weekDay.getValue() - today.getDayOfWeek().getValue();
    if (daysToAdd == 0 || date.isEqual(today)) {
      daysToAdd += 7;
    }

    return today.plusDays(daysToAdd).format(formatter);
  }

  private ChapterDTO setFirstAndLastChapters(ChapterDTO chapterInfo, Document docAnimeLife, Integer chapter) {
    try {
      Element itemFirstChapter = docAnimeLife.body().select(".episodelist ul li").last();
      Element itemLastChapter = docAnimeLife.body().select(".episodelist ul li").first();

      // El nombre del ánime puede estar modificado en casos especiales
      String chapterNameModified = this.animeUtils.specialNameOrUrlCases(chapterInfo.getName(), 'l');

      if (itemFirstChapter != null && itemLastChapter != null) {
        // Imagen del capítulo
        String chapterImg = itemFirstChapter.select(".thumbnel img").attr("src").replace("?resize=130,130", "");
        
        // Primer y último capítulo
        String firstChapter = itemFirstChapter.select(".playinfo h4").text().replace(chapterNameModified, "").trim();
        String lastChapter = itemLastChapter.select(".playinfo h4").text().replace(chapterNameModified, "").trim();
        
        // Redondear el primer o el último capítulo
        if (firstChapter.contains(".")) {
          firstChapter = firstChapter.substring(0, firstChapter.indexOf("."));
        }
        if (lastChapter.contains(".")) {
          lastChapter = lastChapter.substring(0, lastChapter.indexOf("."));
        }

        // Fecha del último capítulo
        String lastChapterDate = itemLastChapter.select(".playinfo span").text();
        String[] chapterDateArray = lastChapterDate.split(" - ");
        lastChapterDate = chapterDateArray[chapterDateArray.length - 1];

        // Asigna la información
        chapterInfo.setChapterImg(chapterImg);
        chapterInfo.setActualChapter(chapter);
        chapterInfo.setFirstChapter(Integer.parseInt(firstChapter));
        chapterInfo.setLastChapter(Integer.parseInt(lastChapter));
        chapterInfo.setLastChapterDate(lastChapterDate);
      }

      return chapterInfo;
    } catch (Exception e) {
      throw new ChapterNotFound("4: " + e.getMessage());
    }
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

package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
@Log
public class ChapterAnimeLifeService {
  @Value("${PROVEEDOR_ANIMELIFE_URL}")
  private String proveedorAnimeLifeUrl;

  public ChapterDTO chapter(String inputName, String chapter) {
    try {
      String urlRequest = proveedorAnimeLifeUrl + AnimeUtils.specialNameOrUrlCases(inputName, 'c');
      urlRequest += "-" + chapter;
      urlRequest = specialChapterCases(urlRequest, inputName, chapter);
      
      Document docChapter = DataUtils.connectAnimeInfo(urlRequest, "No se encontró el capitulo solicitado.");

      List<LinkDTO> srcOptions = this.getSrcOptions(docChapter);
      Elements nearChapters = docChapter.body().select(".naveps .nvs");

      ChapterDTO chapterInfo = ChapterDTO.builder()
        .name(AnimeUtils.specialNameOrUrlCases(docChapter.select(".ts-breadcrumb li").get(1).select("span").text().trim(), 'c'))
        .actualChapterNumber(chapter)
        .srcOptions(srcOptions)
        .downloadOptions(this.getDownloadOptions(docChapter))
        .havePreviousChapter(this.havePreviousChapter(nearChapters))
        .haveNextChapter(this.haveNextChapter(nearChapters))
        .build();
      
      if (!chapterInfo.getHaveNextChapter()) {
        chapterInfo.setNextChapterDate(String.valueOf(this.parseDate(docChapter.body().select(".year .updated").text().trim(), 7)));
      }

      String state = docChapter.body().select(".det").first().select("span i").text().trim();
      if (state.equals("Completada")) {
        chapterInfo.setInEmision(false);
      } else {
        chapterInfo.setInEmision(true);
      }

      Element lastChapter = docChapter.body().select(".episodelist ul li").first().select("a").first();
      // Número del último capitulo
      String chapterNumber = lastChapter.select(".playinfo span").text().split(" - ")[0].replace("Eps ", "");
      chapterInfo.setLastChapterNumber(Integer.parseInt(chapterNumber));
      // Imagen del último capitulo
      String chapterImg = lastChapter.select("img").attr("src");
      chapterInfo.setLastChapterImg(chapterImg);
      // Fecha de salida del último capitulo
      String chapterDate = docChapter.body().select(".updated").text();
      chapterInfo.setLastChapterDate(this.parseDate(chapterDate, 0));

      return chapterInfo;
    } catch (Exception e) {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private List<LinkDTO> getSrcOptions(Document docChapter) {
    try {
      List<LinkDTO> list = new ArrayList<>();
      Elements srcs = docChapter.body().select(".mirror option");
      srcs.remove(0); // Elimina el primer elemento: "Seleccionar servidor"

      for (Element element : srcs) {
        String url = DataUtils.decodeBase64(element.attr("value"), true);

        if (url.startsWith("//")) {
          url = "https://" + url.substring(2);
        }

        LinkDTO link = new LinkDTO();
        link.setName(element.text().trim());
        link.setUrl(url);
        
        list.add(link);
      }
  
      LinkDTO first = list.get(0);
      list.remove(0);
      list.add(first);
  
      return list;
    } catch (Exception e) {
      throw new ChapterNotFound("Ocurrió un error al obtener los servidores de reproducción.");
    }
  }

  private List<LinkDTO> getDownloadOptions(Document docChapter) {
    try {
      Element element = docChapter.body().select(".iconx").first().select("a").first();
      if (element != null) {
        List<LinkDTO> list = new ArrayList<>();
    
        LinkDTO link = LinkDTO.builder()
          .name(this.getProviderName(element.attr("href")))
          .url(element.attr("href"))
          .build();
    
        list.add(link);
    
        return list;
      } else {
        Elements elements = docChapter.body().select(".bixbox .soraurlx a");
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
      throw new ChapterNotFound("Ocurrió un error al obtener los servidores de descarga.");
    }
  }

  private String getProviderName(String url) {
    String regex = "https://(?:www\\.)?([^\\.]+)";
    Pattern pattern = java.util.regex.Pattern.compile(regex);
    Matcher matcher = pattern.matcher(url);
    // https://www.animeflv.net
    // Animeflv
    
    if (matcher.find()) {
        String provider = matcher.group(1);
        return provider.substring(0, 1).toUpperCase() + provider.substring(1);
    } else {
        return "Privado";
    }
  }

  private boolean havePreviousChapter(Elements nearChapters) {
    Element previousChapter = nearChapters.first().select("a").first();

    if (previousChapter != null) {
      return true;
    } else {
      return false;
    }
  }

  private boolean haveNextChapter(Elements nearChapters) {
    Element nextChapter = nearChapters.last().select("a").first();

    if (nextChapter != null) {
      return true;
    } else {
      return false;
    }
  }

  private String parseDate(String date, int daysToSum) {
    if (date == null || date.isEmpty()) {
        return null;
    }

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", new Locale("es", "ES"));

    try {
      LocalDate currentDate = LocalDate.parse(date, inputFormatter);
      LocalDate nextChapterDate = currentDate.plusDays(daysToSum);
      return nextChapterDate.format(outputFormatter);
    } catch (DateTimeParseException e) {
      log.severe("Formato de fecha inválido: " + e.getMessage());
      return null;
    }
  }
  
  private String specialChapterCases(String urlRequest, String inputName, String chapter) {
    if (inputName == null || inputName.isEmpty() || chapter == null) {
      throw new ChapterNotFound("El nombre del anime y el capítulo son obligatorios.");
    }

    Map<String, String> specialCases = new HashMap<>();
    String[] animes = {
      "one-piece",
      "one-punch-man",
      "horimiya",
      "",
      ""
    };

    for (String anime : animes) {
      if (!anime.isEmpty() && anime != null) {
        specialCases.put(anime, "-");
      }
    }

    if (chapter.matches("\\d+")) {
      int chapterNumber = Integer.parseInt(chapter);
      if (chapterNumber < 10) {
        if (specialCases.containsKey(inputName)) {
          // Remplaza la parte final de la URL con la regla especial
          urlRequest = urlRequest.replaceAll("-\\d+$", specialCases.get(inputName) + chapterNumber);
        } else {
          urlRequest = urlRequest.replaceAll("-\\d+$", "-0" + chapterNumber);
        }
      }
    }

    return urlRequest;
  }
  
}

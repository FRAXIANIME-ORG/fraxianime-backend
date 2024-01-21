package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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

@Service
@Log
public class ChapterAnimeLifeService {
  @Value("${PROVEEDOR_ANIMELIFE_URL}")
  private String proveedorAnimeLifeUrl;

  public ChapterDTO chapter(String inputName, String chapter) {
    try {
      String urlRequest = proveedorAnimeLifeUrl + this.specialUrlCases(inputName);
      urlRequest = this.specialChapterCases(urlRequest, inputName, chapter);

      Document docChapter = null;
      docChapter = ZMethods.connectAnimeInfo(docChapter, urlRequest, "No se encontró el capitulo solicitado.");

      List<LinkDTO> srcOptions = this.getSrcOptions(docChapter);
      Elements nearChapters = docChapter.body().select(".naveps .nvs");

      ChapterDTO chapterInfo = ChapterDTO.builder()
        .name(docChapter.select(".ts-breadcrumb li").get(1).select("span").text().trim())
        .actualChapterNumber(chapter)
        .srcOptions(srcOptions)
        .downloadOptions(this.getDownloadOptions(docChapter))
        .havePreviousChapter(this.havePreviousChapter(nearChapters))
        .haveNextChapter(this.haveNextChapter(nearChapters))
        .state(docChapter.body().select(".det").first().select("span i").text().trim())
        .build();
      
      if (!chapterInfo.getHaveNextChapter()) {
        chapterInfo.setNextChapterDate(String.valueOf(this.parseDate(docChapter.body().select(".year .updated").text().trim(), 7)));
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
      log.info("Error 2: " + e.getMessage());
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private List<LinkDTO> getSrcOptions(Document docChapter) {
    try {
      List<LinkDTO> list = new ArrayList<>();
      Elements srcs = docChapter.body().select(".mirror option");
      srcs.remove(0); // Elimina el primer elemento: "Seleccionar servidor"
  
      for (Element element : srcs) {
          String url = ZMethods.decodeBase64(element.attr("value"), true);
  
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

    // Definir el formato de la fecha de entrada en español
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", new Locale("es", "ES"));
    // Formateador para la fecha de salida
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", new Locale("es", "ES"));

    try {
      // Convertir la cadena de fecha de entrada a LocalDate
      LocalDate currentDate = LocalDate.parse(date, inputFormatter);
      // Sumar 7 días para obtener la fecha del próximo capítulo
      LocalDate nextChapterDate = currentDate.plusDays(daysToSum);
      // Convertir la fecha del próximo capítulo a String en el formato deseado
      return nextChapterDate.format(outputFormatter);
    } catch (DateTimeParseException e) {
      // Manejar la excepción si la cadena de fecha no es válida
      log.severe("Formato de fecha inválido: " + e.getMessage());
      return null;
    }
  }

  public String specialUrlCases(String inputName) {
    if (inputName.isEmpty() || inputName == null) {
      throw new ChapterNotFound("El nombre del anime no puede estar vacio.");
    }

    Map<String, String> specialCases = new HashMap<>();

    specialCases.put("ore-dake-level-up-na-ken", "solo-leveling");

    for (Map.Entry<String, String> entry : specialCases.entrySet()) {
        if (inputName.contains(entry.getKey())) {
            return entry.getValue();
        }
    }

    return inputName;
  }

  private String specialChapterCases(String urlRequest, String inputName, String chapter) {
    if (inputName.isEmpty() || inputName == null || chapter == null) {
      throw new ChapterNotFound("El nombre del anime y el capitulo son obligatorios.");
    }

    Map<String, String> specialCases = new HashMap<>();
    // Por defecto, los primeros 9 episodios de TODOS los animes siguen esta regla: -0X
    // Pero hay algunos animes que tienen una regla especial para los primeros 9 episodios: -X
    specialCases.put("one-piece", "-");

    // Si el capitulo es un número, las otras opciones son: OVA, ONA, Pelicula, Especial, etc.
    if (chapter.matches("\\d+")) {
      for (Entry<String, String> entry : specialCases.entrySet()) {
        if (Integer.parseInt(chapter) < 10) {
          if (entry.getKey().equals(inputName)) {
            urlRequest += entry.getValue() + chapter;
          } else {
            urlRequest += "-0" + chapter;
          }
        } else {
          urlRequest += "-" + chapter;
        }
      }
    }

    return urlRequest;
  }
  
}

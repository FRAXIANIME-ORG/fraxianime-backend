package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.SpecificChapterDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;

@Service
public class SpecificChapterService {
  @Value("${BASE_URL}")
  private String baseUrl;

  public SpecificChapterDTO specificChapter(String inputName, Integer chapter) {
    String urlBase = this.baseUrl;
    String urlSpecificChapter = urlBase + (inputName + "-episodio-" + chapter);
    String urlAnimeInfo = urlBase + inputName;

    Document docSpecificChapter = null;
    Document docAnimeInfo = null;
    
    docSpecificChapter = ZMethods.connectAnimeInfo(docSpecificChapter, urlSpecificChapter, "No se encontró el capitulo solicitado.");
    docAnimeInfo = ZMethods.connectAnimeInfo(docAnimeInfo, urlAnimeInfo, "No se encontró el anime solicitado.");

    List<LinkDTO> srcOptions = this.getSrcOptions(docSpecificChapter);
    List<LinkDTO> downloadOptions = this.getDownloadOptions(docSpecificChapter);

    SpecificChapterDTO animeInfo = SpecificChapterDTO.builder()
      .name(docSpecificChapter.select(".entry-title").first().text().replace(("Episodio " + chapter), "").trim() + " - " + chapter)
      .srcOptions(srcOptions)
      .downloadOptions(downloadOptions)
      .previousChapterUrl(this.previousChapterUrl(urlAnimeInfo, chapter))
      .nextChapterUrl(this.nextChapterUrl(urlAnimeInfo, chapter, this.getNumberOfChapters(docAnimeInfo, chapter)))
      .build();

    return animeInfo;
  }

  private List<LinkDTO> getSrcOptions(Document docSpecificChapter) {
    List<LinkDTO> list = new ArrayList<>();
    Elements srcs = docSpecificChapter.select(".iframe_code");
    int counter = 1;

    for (Element element : srcs) {
      LinkDTO link = new LinkDTO();
      link.setUrl(this.decodeBase64(element.attr("data-src")));
      link.setName(this.getProviderName(link.getUrl(), counter));
      
      counter++;
      list.add(link);
    }

    // pasar Streamwish al final de la lista
    LinkDTO first = list.get(0);
    list.remove(0);
    list.add(first);

    return list;
  }

  private List<LinkDTO> getDownloadOptions(Document docSpecificChapter) {
    List<LinkDTO> list = new ArrayList<>();
    Elements downloads = docSpecificChapter.select(".styled-table tbody tr");
    int counter = 1;

    for (Element element : downloads) {
      String downloadLink = element.select("td").last().select("a").attr("href");
      LinkDTO link = new LinkDTO();
      link.setUrl(downloadLink);
      link.setName(this.getProviderName(link.getUrl(), counter));
      
      counter++;
      list.add(link);
    }

    return list;
  }

  private String decodeBase64(String encodedString) {
    return new String(Base64.getDecoder().decode(encodedString));
  }

  private String getProviderName(String url, int optionNumber) {
    String regex = "https://(?:www\\.)?([^\\.]+)";
    Pattern pattern = java.util.regex.Pattern.compile(regex);
    Matcher matcher = pattern.matcher(url);

    if (matcher.find()) {
        String provider = matcher.group(1);
        return provider.substring(0, 1).toUpperCase() + provider.substring(1);
    } else {
        return "Opcion " + optionNumber;
    }
  }

  private int getNumberOfChapters(Document document, Integer actualChapter) {
    Element element = document.select(".fa-play-circle").first();

    // Si la pagina no tiene capitulos, retorna que la cantidad de capitulos es la misma que el capitulo actual
    if (element == null) {
      return actualChapter;
    }

    String lastCap = element.select("a h3").text();
    Integer lastCapNumber;

    if (lastCap != null && !lastCap.isEmpty()) {
      lastCapNumber = Integer.parseInt(lastCap.replaceAll("\\D+", ""));
    } else {
      lastCapNumber = 0;
    }

    if (lastCapNumber > 0) {
      return lastCapNumber;
    } else {
      return 0;
    }
  }

  private String previousChapterUrl(String urlAnimeInfo, int actualChapter) {;
    if (actualChapter > 1) {
      return (urlAnimeInfo + "/" + (actualChapter - 1)).replace(this.baseUrl, "");
    } else if (actualChapter == 1) {
      return null;
    } else {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private String nextChapterUrl(String urlAnimeInfo, int actualChapter, int chapters) {
    // Si la pagina no tiene capitulos, retorna el link
    // del siguiente capitulo pero con esto al final: #notsecure
    int chaptersModified = chapters;
    if (chapters == 0) {
      chaptersModified = actualChapter + 1;
    }

    // La pagina puede no tener capitulos porque animeflv.com.ru a veces
    // se buggea y no muestra los capitulos en la pagina del anime xd
    if (actualChapter < chaptersModified) {
      String nextChapterUrl = (urlAnimeInfo + "/" + (actualChapter + 1)).replace(this.baseUrl, "");
      if (chapters == 0) {
        return nextChapterUrl + "#notsecure";
      } else {
        return nextChapterUrl;
      }
    } else if (actualChapter == chaptersModified) {
      return null;
    } else {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }
    
}

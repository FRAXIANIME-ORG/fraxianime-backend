package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
public class ChapterAnimeLifeService {
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;

  @Autowired
  private AnimeUtils animeUtils;

  public ChapterDTO chapter(Document docAnimeLife, String name, Integer chapter) {
    try {

      List<LinkDTO> srcOptions = this.getSrcOptions(docAnimeLife);
      Elements nearChapters = docAnimeLife.body().select(".naveps .nvs");

      ChapterDTO chapterInfo = ChapterDTO.builder()
        .name(this.animeUtils.specialNameOrUrlCases(docAnimeLife.select(".ts-breadcrumb li").get(1).select("span").text().trim(), 'n'))
        .srcOptions(srcOptions)
        .downloadOptions(this.getDownloadOptions(docAnimeLife))
        .havePreviousChapter(this.havePreviousChapter(nearChapters))
        .haveNextChapter(this.haveNextChapter(nearChapters))
        .build();
      
      if (!chapterInfo.getHaveNextChapter()) {
        chapterInfo.setNextChapterDate(DataUtils.parseDate(docAnimeLife.body().select(".year .updated").text().trim(), 7));
      }

      String state = docAnimeLife.body().select(".det").first().select("span i").text().trim();
      if (state.equals("Completada")) {
        chapterInfo.setInEmision(false);
      } else {
        chapterInfo.setInEmision(true);
      }

      chapterInfo = this.setFirstAndLastChapters(chapterInfo, docAnimeLife, chapter);

      return chapterInfo;
    } catch (Exception e) {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private List<LinkDTO> getSrcOptions(Document docAnimeLife) {
    try {
      List<LinkDTO> list = new ArrayList<>();
      Elements srcs = docAnimeLife.body().select(".mirror option");
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
  
      for(int i = 0; i < list.size(); i++) {
        if (list.get(i).getName().equals("YourUpload")) {
          LinkDTO link = list.get(i);
          list.remove(i);
          list.add(0, link);
        }
        if (list.get(i).getName().equals("VidGuard")) {
          LinkDTO link = list.get(i);
          list.remove(i);
          list.add(0, link);
        }
      }
  
      return list;
    } catch (Exception e) {
      throw new ChapterNotFound("Ocurrió un error al obtener los servidores de reproducción.");
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
      throw new ChapterNotFound("Ocurrió un error al obtener los servidores de descarga.");
    }
  }

  private ChapterDTO setFirstAndLastChapters(ChapterDTO chapterInfo, Document docAnimeLife, Integer chapter) {
    try {
      Element itemFirstChapter = docAnimeLife.body().select(".episodelist ul li").last();
      Element itemLastChapter = docAnimeLife.body().select(".episodelist ul li").first();

      if (itemFirstChapter != null && itemLastChapter != null) {
        // Ambos (img)
        String chapterImg = itemFirstChapter.select(".thumbnel img").attr("src").replace("?resize=130,130", "");
        // Ambos (número)
        String firstChapter = itemFirstChapter.select(".playinfo h4").text().replace(chapterInfo.getName(), "").trim();
        String lastChapter = itemLastChapter.select(".playinfo h4").text().replace(chapterInfo.getName(), "").trim();
        // Último capítulo (fecha)
        String lastChapterDate = itemLastChapter.select(".playinfo span").text();
        String[] chapterDateArray = lastChapterDate.split(" - ");
        lastChapterDate = chapterDateArray[chapterDateArray.length - 1];
  
        chapterInfo.setChapterImg(chapterImg);
        chapterInfo.setActualChapter(chapter);
        chapterInfo.setFirstChapter(Integer.parseInt(firstChapter));
        chapterInfo.setLastChapter(Integer.parseInt(lastChapter));
        chapterInfo.setLastChapterDate(lastChapterDate);
      }

      return chapterInfo;
    } catch (Exception e) {
      throw new AnimeNotFound("Error obteniendo la información de los capítulos del anime.");
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

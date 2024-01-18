package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.AnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.SpecificChapterDTO;

@Service
@Log
public class JKAnimeService {
  private String scrappingUrl = "https://animeflv.com.ru/anime/";

  public List<AnimeDTO> lastChapters(Document document) {
    Elements elements = document.select(".ht_grid_1_4");
    List<AnimeDTO> lastChapters = new ArrayList<>();

    for (Element element : elements) {
      String imgUrl = element.select(".thumbnail-wrap img").attr("src");
      String chapter = element.select(".ep-number").text().replace("Episodio", "Capitulo");
      String name = element.select(".entry-title").text().replace(".", "")
        .replaceAll(" Episodio \\d+", "").trim();
      String url = element.select(".thumbnail-link").attr("href")
        .replace(this.scrappingUrl, "")
        .replace("-episodio-", "/");
      
      if (
        isNotNullOrEmpty(imgUrl) && isNotNullOrEmpty(chapter) &&
        isNotNullOrEmpty(name) && isNotNullOrEmpty(url)
      ) {
        AnimeDTO anime = AnimeDTO.builder()
          .imgUrl(imgUrl)
          .name(name)
          .chapter(chapter)
          .url(url)
          .build();
        
        lastChapters.add(anime);
      }
    }

    return lastChapters;
  }

  public List<AnimeDTO> allAnimes(Document document) {
    Elements elements = document.select(".ht_grid_1_5");
    List<AnimeDTO> allAnimes = new ArrayList<>();

    for (Element element : elements) {
      String imgUrl = element.select(".anime-image").attr("src");
      String name = element.select(".entry-title").text().replace(".", "").trim();
      String url = element.select(".thumbnail-link").attr("href")
        .replace(this.scrappingUrl, "")
        .replace("-episodio-", "/");
      
      if (
        isNotNullOrEmpty(imgUrl) && isNotNullOrEmpty(name) &&
        isNotNullOrEmpty(url)
      ) {
        AnimeDTO anime = AnimeDTO.builder()
          .imgUrl(imgUrl)
          .name(name)
          .url(url)
          .build();
        
        allAnimes.add(anime);
      }
    }

    return allAnimes;
  }

  public List<AnimeDTO> emisionAnimes(Document document) {
    Elements elements = document.select("#left-menu li");
    List<AnimeDTO> emisionAnimes = new ArrayList<>();

    for (Element element : elements) {
      String name = element.select("a").attr("title").replace(".", "").trim();
      String url = element.select("a").attr("href")
        .replace(this.scrappingUrl, "")
        .replace("-episodio-", "/");

      if (isNotNullOrEmpty(name) && isNotNullOrEmpty(url)) {
        AnimeDTO anime = AnimeDTO.builder()
          .name(name)
          .url(url)
          .build();
        
        emisionAnimes.add(anime);
      }
    }

    return emisionAnimes;
  }

  public SpecificChapterDTO specificChapter(String inputName, Integer chapter) {
    String urlBase = this.scrappingUrl;
    String urlSpecificChapter = urlBase + (inputName + "-episodio-" + chapter);
    String urlAnimeInfo = urlBase + inputName;

    Document docSpecificChapter = null;
    Document docAnimeInfo = null;
    docSpecificChapter = this.connectAnimeInfo(docSpecificChapter, urlSpecificChapter, "No se encontró el capitulo solicitado.");
    docAnimeInfo = this.connectAnimeInfo(docAnimeInfo, urlAnimeInfo, "No se encontró el anime solicitado.");

    int episodes = this.getNumberOfEpisodes(docAnimeInfo);

    SpecificChapterDTO animeInfo = SpecificChapterDTO.builder()
      .name(docAnimeInfo.select(".Title").first().text())
      .iframeSrc(docSpecificChapter.select(".iframe-container iframe").attr("src"))
      .previousChapterUrl(this.previousChapterUrl(urlAnimeInfo, chapter))
      .nextChapterUrl(this.nextChapterUrl(urlAnimeInfo, chapter, episodes))
      .build();

    return animeInfo;
  }

  public AnimeInfoDTO getAnimeInfo(String search) {
    String urlBase = this.scrappingUrl;
    String urlAnimeInfo = urlBase + search.replaceAll("/\\d+$", "");

    Document docAnimeInfo = null;
    docAnimeInfo = this.connectAnimeInfo(docAnimeInfo, urlAnimeInfo, "No se encontró el anime solicitado.");
    
    String chaptersImgUrl = docAnimeInfo.select(".lazy").attr("src");
    AnimeInfoDTO animeInfo = AnimeInfoDTO.builder()
      .name(docAnimeInfo.select(".Title").first().text().trim())
      .sinopsis(docAnimeInfo.select(".Description p").text())
      .imgUrl(docAnimeInfo.select(".anime-image").attr("src"))
      .chapters(this.getChapters(docAnimeInfo, chaptersImgUrl))
      .genres(this.getGenres(docAnimeInfo))
      .build();

    return animeInfo;
  }

  private List<AnimeDTO> getChapters(Document document, String chaptersImgUrl) {
    Elements elements = document.select(".fa-play-circle");
    List<AnimeDTO> chapters = new ArrayList<>();

    for (Element element : elements) {
      String url = element.select("a").attr("href")
        .replace(this.scrappingUrl, "")
        .replace("-episodio-", "/").trim();
      String chapter = "Capitulo " + this.getChapterNumberFromUrl(url);
      
      if (isNotNullOrEmpty(url)) {
        AnimeDTO anime = AnimeDTO.builder()
          .url(url)
          .chapter(chapter)
          .imgUrl(chaptersImgUrl)
          .build();
        
        chapters.add(anime);
      }
    }

    return chapters;
  }

  private List<String> getGenres(Document document) {
    Elements elements = document.select(".Nvgnrs a");
    List<String> genres = new ArrayList<>();

    for (Element element : elements) {
      genres.add(element.text());
    }

    return genres;
  }

  private Document connectAnimeInfo(Document docAnimeInfo, String urlAnimeInfo, String errorMessage) {
    try {
      return docAnimeInfo = Jsoup.connect(urlAnimeInfo).get();
    } catch (Exception e) {
      log.warning(errorMessage + ": " + e.getMessage());
      throw new AnimeNotFound(errorMessage);
    }
  }

  private String previousChapterUrl(String urlAnimeInfo, int actualChapter) {
    if (actualChapter > 1) {
      return (urlAnimeInfo + "/" + (actualChapter - 1)).replace(this.scrappingUrl, "");
    } else if (actualChapter == 1) {
      return null;
    } else {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private String nextChapterUrl(String urlAnimeInfo, int actualChapter, Integer chapters) {
    if (actualChapter < chapters) {
      return (urlAnimeInfo + "/" + (actualChapter + 1)).replace(this.scrappingUrl, "");
    } else if (actualChapter == chapters) {
      return null;
    } else {
      throw new ChapterNotFound("No se encontró el capitulo solicitado.");
    }
  }

  private boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }

  private int getChapterNumberFromUrl(String uri) {
    return Integer.parseInt(uri.replaceAll(".*/(\\d+)", "$1"));
  }

  private int getNumberOfEpisodes(Document document) {
    Elements caps = document.select(".fa-play-circle");
    if (caps.size() > 0) {
      return caps.size();
    } else {
      return 0;
    }
  }

}

package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDTO;

@Service
public class HomePageService {
  @Value("${BASE_URL}")
  private String baseUrl;

  public List<AnimeDTO> lastChapters(Document document) {
    Elements elements = document.select(".ht_grid_1_4");
    List<AnimeDTO> lastChapters = new ArrayList<>();

    for (Element element : elements) {
      String imgUrl = element.select(".thumbnail-wrap img").attr("src");
      String chapter = element.select(".ep-number").text().replace("Episodio", "Capitulo");
      String name = element.select(".entry-title").text().replace(".", "")
        .replaceAll(" Episodio \\d+", "").trim();
      String url = element.select(".thumbnail-link").attr("href")
        .replace(this.baseUrl, "")
        .replace("-episodio-", "/");
      
      if (
        ZMethods.isNotNullOrEmpty(imgUrl) && ZMethods.isNotNullOrEmpty(chapter) &&
        ZMethods.isNotNullOrEmpty(name) && ZMethods.isNotNullOrEmpty(url)
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
        .replace(this.baseUrl, "")
        .replace("-episodio-", "/");
      
      if (
        ZMethods.isNotNullOrEmpty(imgUrl) && ZMethods.isNotNullOrEmpty(name) &&
        ZMethods.isNotNullOrEmpty(url)
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
        .replace(this.baseUrl, "")
        .replace("-episodio-", "/");

      if (ZMethods.isNotNullOrEmpty(name) && ZMethods.isNotNullOrEmpty(url)) {
        AnimeDTO anime = AnimeDTO.builder()
          .name(name)
          .url(url)
          .build();
        
        emisionAnimes.add(anime);
      }
    }

    return emisionAnimes;
  }
    
}

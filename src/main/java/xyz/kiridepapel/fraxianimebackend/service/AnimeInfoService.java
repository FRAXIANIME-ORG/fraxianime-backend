package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDTO;
import xyz.kiridepapel.fraxianimebackend.dto.AnimeInfoDTO;

@Service
public class AnimeInfoService {
  @Value("${BASE_URL}")
  private String baseUrl;

  public AnimeInfoDTO getAnimeInfo(String search) {
    String urlBase = this.baseUrl;
    String urlAnimeInfo = urlBase + search.replaceAll("/\\d+$", "");

    Document docAnimeInfo = null;
    docAnimeInfo = ZMethods.connectAnimeInfo(docAnimeInfo, urlAnimeInfo, "No se encontró el anime solicitado.");
    
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
        .replace(this.baseUrl, "")
        .replace("-episodio-", "/").trim();
      String chapter = "Capitulo " + this.getChapterNumberFromUrl(url);
      
      if (ZMethods.isNotNullOrEmpty(url)) {
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

  private int getChapterNumberFromUrl(String uri) {
    return Integer.parseInt(uri.replaceAll(".*/(\\d+)", "$1"));
  }
    
}

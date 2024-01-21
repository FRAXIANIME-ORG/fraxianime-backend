package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDTO;

@Service
public class HomePageService {
  @Value("${PROVEEDOR_ALL_URL}")
  private String proveedorAllUrl;

  public List<ChapterDTO> sliderAnimes(Document document) {
    Elements elements = document.select(".hero__items");
    List<ChapterDTO> sliderAnimes = new ArrayList<>();
    
    for (Element element : elements) {
      ChapterDTO anime = ChapterDTO.builder()
        .name(element.select(".hero__text h2").text())
        .imgUrl(element.attr("data-setbg"))
        .url(element.select(".hero__text a").attr("href").replace(proveedorAllUrl, ""))
        .build();

      sliderAnimes.add(anime);
    }

    return sliderAnimes;
  }

  public List<LastAnimeInfoDTO> ovasOnasSpecials(Document document) {
    Elements elements = document.select(".solopc").last().select(".anime__item");
    List<LastAnimeInfoDTO> ovasOnasSpecials = new ArrayList<>();

    for (Element element : elements) {
      LastAnimeInfoDTO anime = LastAnimeInfoDTO.builder()
        .name(element.select(".anime__item__text a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(proveedorAllUrl, ""))
        .type(element.select(".anime__item__text ul li").text())
        .build();

      ovasOnasSpecials.add(anime);
    }

    return ovasOnasSpecials;
  }

  public List<ChapterDTO> genericProgramming(Document document, char type) {
    Elements elements = document.select(".anime_programing a.bloqq");
    List<ChapterDTO> lastChapters = new ArrayList<>();
    String formattedDate;

    if (type == 'a') {
      elements = document.select(".anime_programing a.bloqq");
    } else if (type == 'd') {
      elements = document.select(".donghuas_programing a.bloqq");
    }

    for (Element element : elements) {
      String dateText = element.select(".anime__sidebar__comment__item__text span").text();

      if (dateText.equals("Hoy") || dateText.equals("Ayer")) {
        // Manejar el caso de que la fecha sea Hoy o Ayer
        formattedDate = dateText;
      } else if (dateText.matches("\\d{2}/\\d{2}")) {
        // Manejar el caso de que la fecha sea DIA/MES
        String currentYear = String.valueOf(LocalDate.now().getYear());
        LocalDate date = LocalDate.parse((dateText + "/" + currentYear), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        long daysBetween = ChronoUnit.DAYS.between(date, LocalDate.now());

        if (daysBetween <= 7) {
          formattedDate = "Hace " + daysBetween + " días";
        } else {
          formattedDate = dateText;
        }
      } else { 
        // Manejar otros casos si los hay
        formattedDate = dateText;
      }

      ChapterDTO anime = ChapterDTO.builder()
        .name(element.select(".anime__sidebar__comment__item__text h5").text())
        .imgUrl(element.select(".anime__sidebar__comment__item__pic img").attr("src"))
        .chapter(element.select(".anime__sidebar__comment__item__text h6").text())
        .date(formattedDate)
        .url(element.attr("href").replace(proveedorAllUrl, ""))
        .build();

      lastChapters.add(anime);
    }

    return lastChapters;
  }

  public List<TopDTO> topAnimes(Document document) {
    Element data = document.select(".destacados").last().select(".container div").first();
    List<TopDTO> topAnimes = new ArrayList<>(10);

    Element firstTop = data.child(2);
    TopDTO firstAnime = TopDTO.builder()
      .name(firstTop.select(".comment h5").text())
      .imgUrl(firstTop.select(".anime__item__pic").attr("data-setbg"))
      .likes(Integer.parseInt(firstTop.select(".vc").text().trim()))
      .position(Integer.parseInt(firstTop.select(".ep").text().trim()))
      .url(firstTop.select("a").attr("href").replace(proveedorAllUrl, ""))
      .build();
    topAnimes.add(firstAnime);

    Elements restTop = data.child(3).select("a");
    for (Element element : restTop) {
      TopDTO anime = TopDTO.builder()
        .name(element.select(".comment h5").text())
        .imgUrl(element.select(".anime__item__pic__fila4").attr("data-setbg"))
        .likes(Integer.parseInt(element.select(".vc").text()))
        .position(Integer.parseInt(element.select(".anime__item__pic__fila4 div").first().text().trim()))
        .url(element.attr("href").replace(proveedorAllUrl, ""))
        .build();

      topAnimes.add(anime);
    }

    return topAnimes;
  }

  public List<LastAnimeInfoDTO> latestAddedAnimes(Document document) {
    Elements elements = document.select(".trending__anime .anime__item");
    List<LastAnimeInfoDTO> latestAddedAnimes = new ArrayList<>();

    for (Element element : elements) {
      LastAnimeInfoDTO anime = LastAnimeInfoDTO.builder()
        .name(element.select(".anime__item__text h5 a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(proveedorAllUrl, ""))
        .state(element.select(".anime__item__text ul li").first().text())
        .type(element.select(".anime__item__text ul li").last().text())
        .build();

      latestAddedAnimes.add(anime);
    }

    return latestAddedAnimes;
  }

  public List<LinkDTO> latestAddedList(Document document) {
    Elements elements = document.select(".trending_div .side-menu li");
    List<LinkDTO> latestAddedList = new ArrayList<>();

    for (Element element : elements) {
      LinkDTO anime = LinkDTO.builder()
        .name(element.select("a").text())
        .url(element.select("a").attr("href").replace(proveedorAllUrl, ""))
        .build();

      latestAddedList.add(anime);
    }

    return latestAddedList;

  }
    
}

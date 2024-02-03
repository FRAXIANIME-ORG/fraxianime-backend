package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
@Log
public class HomePageService {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private DataUtils dataUtils;
  @Autowired
  AnimeUtils animeUtils;

  @Cacheable(value = "home", key="'animes'")
  public HomePageDTO homePage() {
    Document docJkanime = this.dataUtils.simpleConnect(this.providerJkanimeUrl, "Proveedor 1 inactivo");
    Document docAnimelife = this.dataUtils.simpleConnect(this.providerAnimeLifeUrl, "Proveedor 2 inactivo");

    HomePageDTO animes = HomePageDTO.builder()
      .sliderAnimes(this.sliderAnimes(docJkanime))
      .ovasOnasSpecials(this.ovasOnasSpecials(docJkanime))
      .animesProgramming(this.animesProgramming(docAnimelife, docJkanime))
      .donghuasProgramming(this.donghuasProgramming(docJkanime))
      .topAnimes(this.topAnimes(docJkanime))
      .latestAddedAnimes(this.latestAddedAnimes(docJkanime))
      .latestAddedList(this.latestAddedList(docJkanime))
      .build();
    
    return animes;
  }

  public List<ChapterDataDTO> sliderAnimes(Document document) {
    Elements elements = document.select(".hero__items");
    List<ChapterDataDTO> sliderAnimes = new ArrayList<>();
    
    for (Element element : elements) {
      ChapterDataDTO anime = ChapterDataDTO.builder()
        .name(element.select(".hero__text h2").text())
        .imgUrl(element.attr("data-setbg"))
        .url(element.select(".hero__text a").attr("href").replace(providerJkanimeUrl, ""))
        .build();

      sliderAnimes.add(anime);
    }

    return sliderAnimes;
  }

  public List<AnimeDataDTO> ovasOnasSpecials(Document document) {
    Elements elements = document.select(".solopc").last().select(".anime__item");
    List<AnimeDataDTO> ovasOnasSpecials = new ArrayList<>();

    for (Element element : elements) {
      AnimeDataDTO anime = AnimeDataDTO.builder()
        .name(element.select(".anime__item__text a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(providerJkanimeUrl, "").split("/")[0].trim())
        .type(element.select(".anime__item__text ul li").text())
        .build();

      ovasOnasSpecials.add(anime);
    }

    return ovasOnasSpecials;
  }
  
  public List<ChapterDataDTO> animesProgramming(Document docAnimeLife, Document docJkanime) {
    Elements elementsAnimeLife = docAnimeLife.body().select(".excstf").first().select(".bs");
    Elements elementsJkAnime = docJkanime.body().select(".listadoanime-home .anime_programing a");

    List<ChapterDataDTO> lastChapters = new ArrayList<>();
    Map<String, LinkDTO> animesJkanimes = new HashMap<>();

    for (Element eJkanime : elementsJkAnime) {
      String name = eJkanime.select("h5").first().text().trim();
      LinkDTO data = LinkDTO.builder()
        .name(eJkanime.select(".anime__sidebar__comment__item__text span").first().text().trim())
        .url(eJkanime.select(".anime__sidebar__comment__item__pic img").attr("src").trim())
        .build();
      animesJkanimes.put(name, data);
    }

    for (Element eAnimeLife : elementsAnimeLife) {
      String url = this.changeFormatUrl(eAnimeLife.select(".bsx a").attr("href"), providerAnimeLifeUrl);

      if (url.contains("/")) {
        ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(eAnimeLife.select(".tt").first().childNodes().stream()
            .filter(node -> !(node instanceof Element && ((Element) node).tag().getName().equals("h2")))
            .map(Node::toString)
            .collect(Collectors.joining()).trim())
          .imgUrl(eAnimeLife.select("img").attr("src").trim())
          .chapter(eAnimeLife.select(".epx").text().replace("Ep 0", "Capitulo ").replace("Ep ", "Capitulo ").trim())
          .type(eAnimeLife.select(".typez").text().trim())
          .date(null)
          .url(this.changeFormatUrl(eAnimeLife.select(".bsx a").attr("href"), providerAnimeLifeUrl))
          .state(true)
          .build();
        
        String animeName = anime.getName().trim().replace("“", String.valueOf('"')).replace("”", String.valueOf('"'));
              
        anime.setName(this.animeUtils.specialNameOrUrlCases(animeName, 'h'));
        anime.setUrl(this.animeUtils.specialNameOrUrlCases(anime.getUrl(), 'h'));

        if (animesJkanimes.containsKey(animeName)) {
          anime.setDate(this.getFormattedDate(animesJkanimes.get(animeName).getName()));
          anime.setImgUrl(animesJkanimes.get(animeName).getUrl());
        }
      
        lastChapters.add(anime);
      }
    }

    return lastChapters;
  }

  public List<ChapterDataDTO> donghuasProgramming(Document document) {
    Elements elements = document.select(".donghuas_programing a.bloqq");
    List<ChapterDataDTO> lastChapters = new ArrayList<>();

    for (Element element : elements) {
      ChapterDataDTO anime = ChapterDataDTO.builder()
        .name(element.select(".anime__sidebar__comment__item__text h5").text())
        .imgUrl(element.select(".anime__sidebar__comment__item__pic img").attr("src"))
        .chapter(element.select(".anime__sidebar__comment__item__text h6").text().replace("Episodio", "Capitulo"))
        .type("Donghua")
        .date(this.getFormattedDate(element.select(".anime__sidebar__comment__item__text span").text()))
        .url(element.select("a").attr("href").replace(providerJkanimeUrl, ""))
        .state(true)
        .build();
      
      // Quitar el "/" final de la url
      if (anime.getUrl().endsWith("/")) {
        anime.setUrl(anime.getUrl().substring(0, anime.getUrl().length() - 1));
      }
      
      anime.setName(this.animeUtils.specialNameOrUrlCases(anime.getName(), 'h'));
      anime.setUrl(this.animeUtils.specialNameOrUrlCases(anime.getUrl(), 'h'));

      lastChapters.add(anime);
    }

    return lastChapters;
  }

  public List<TopDataDTO> topAnimes(Document document) {
    Element data = document.select(".destacados").last().select(".container div").first();
    List<TopDataDTO> topAnimes = new ArrayList<>(10);

    Element firstTop = data.child(2);
    TopDataDTO firstAnime = TopDataDTO.builder()
      .name(firstTop.select(".comment h5").text())
      .imgUrl(firstTop.select(".anime__item__pic").attr("data-setbg"))
      .likes(Integer.parseInt(firstTop.select(".vc").text().trim()))
      .position(Integer.parseInt(firstTop.select(".ep").text().trim()))
      .url(firstTop.select("a").attr("href").replace(providerJkanimeUrl, ""))
      .build();
    topAnimes.add(firstAnime);

    Elements restTop = data.child(3).select("a");
    for (Element element : restTop) {
      TopDataDTO anime = TopDataDTO.builder()
        .name(element.select(".comment h5").text())
        .imgUrl(element.select(".anime__item__pic__fila4").attr("data-setbg"))
        .likes(Integer.parseInt(element.select(".vc").text()))
        .position(Integer.parseInt(element.select(".anime__item__pic__fila4 div").first().text().trim()))
        .url(element.attr("href").replace(providerJkanimeUrl, ""))
        .build();

      topAnimes.add(anime);
    }

    return topAnimes;
  }

  public List<AnimeDataDTO> latestAddedAnimes(Document document) {
    Elements elements = document.select(".trending__anime .anime__item");
    List<AnimeDataDTO> latestAddedAnimes = new ArrayList<>();

    for (Element element : elements) {
      AnimeDataDTO anime = AnimeDataDTO.builder()
        .name(element.select(".anime__item__text h5 a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(providerJkanimeUrl, ""))
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
        .url(element.select("a").attr("href").replace(providerJkanimeUrl, ""))
        .build();

      latestAddedList.add(anime);
    }

    return latestAddedList;
  }

  private String getFormattedDate(String dateText) {
    if (dateText.equals("Hoy") || dateText.equals("Ayer")) {
      // Manejar el caso de que la fecha sea Hoy o Ayer
      return dateText;
    } else if (dateText.matches("\\d{2}/\\d{2}")) {
      // Manejar el caso de que la fecha sea DIA/MES
      String currentYear = String.valueOf(LocalDate.now().getYear());
      LocalDate date = LocalDate.parse((dateText + "/" + currentYear), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      long daysBetween = ChronoUnit.DAYS.between(date, LocalDate.now());

      if (daysBetween <= 7) {
        if (daysBetween == 1) {
          return "Ayer";
        } else {
          return "Hace " + daysBetween + " días";
        }
      } else {
        return dateText;
      }
    } else { 
      // Manejar otros casos si los hay
      return dateText;
    }
  }

  private String changeFormatUrl(String url, String providerUrl) {
    String newUrl = url.replace(providerUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
    // Verifica si la URL termina con el patrón -xx-2
    if (url.matches(".*-\\d{2}-2/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(\\d{2})-2/?$", "$1");
      try {
          int number = Integer.parseInt(numberPart) + 1;
          newUrl = url.replaceFirst("-\\d{2}-2/?$", "/" + number);
          return newUrl.replace(providerUrl, "");
      } catch (NumberFormatException e) {
          log.warning("Error parsing number from URL: " + url);
      }
    } else {
      // Si no termina con -xx-2, solo elimina los ceros a la izquierda
      newUrl = url.replace(providerUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
      return newUrl;
    }

    return newUrl;
  }

}

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
public class HomeService {
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private DataUtils dataUtils;
  @Autowired
  AnimeUtils animeUtils;

  @Cacheable(value = "home", key = "'animes'")
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

      String[] urlSplit = anime.getUrl().split("/");
      anime.setChapter(urlSplit[urlSplit.length - 1]);

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

    List<ChapterDataDTO> animesProgramming = new ArrayList<>();
    Map<String, ChapterDataDTO> animesJkanimes = new HashMap<>();

    // Obtener los animes de Jkanime
    String year = String.valueOf(LocalDate.now().getYear());
    for (Element item : elementsJkAnime) {
      String date = item.select(".anime__sidebar__comment__item__text span").first().text().trim();

      if (date.equals("Hoy") || date.equals("Ayer")) {
        date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      } else {
        date = DataUtils.parseDate(date + "/" + year, "dd/MM/yyyy", 0);
      }

      ChapterDataDTO data = ChapterDataDTO.builder()
          .name(item.select("h5").first().text().trim())
          .date(date)
          .url(item.select(".anime__sidebar__comment__item__pic img").attr("src").trim())
          .build();
      animesJkanimes.put(data.getName(), data);
    }

    // Obtener los animes de AnimeLife
    int index = 0;
    for (Element item : elementsAnimeLife) {
      String url = this.changeFormatUrl(item.select(".bsx a").attr("href"), providerAnimeLifeUrl);

      if (url.contains("/")) {
        ChapterDataDTO anime = ChapterDataDTO.builder()
            .name(item.select(".tt").first().childNodes().stream()
                .filter(node -> !(node instanceof Element && ((Element) node).tag().getName().equals("h2")))
                .map(Node::toString)
                .collect(Collectors.joining()).trim())
            .imgUrl(item.select("img").attr("src").trim())
            .chapter(item.select(".epx").text().replace("Ep 0", "").replace("Ep ", "").trim())
            .type(item.select(".typez").text().trim())
            .url(this.changeFormatUrl(item.select(".bsx a").attr("href"), providerAnimeLifeUrl))
            .build();

        anime = this.defineSpecialCases(anime);

        if (animesJkanimes.containsKey(anime.getName())) {
          // Si el anime está en Jkanime, usa la fecha y la imagen de Jkanime
          anime.setDate(this.getFormattedDate(animesJkanimes.get(anime.getName()).getDate()));
          anime.setImgUrl(animesJkanimes.get(anime.getName()).getUrl());
          anime.setState(true);
        } else {
          // Si el anime no está en Jkanime, asigna una fecha a partir de la posición
          // en del anime en la lista de animes de AnimeLife
          if (index <= 10)
            anime.setDate("Hoy");
          else if (index <= 15)
            anime.setDate("Ayer");
          else if (index <= 20)
            anime.setDate("Hace 2 días");
          else if (index <= 25)
            anime.setDate("Hace 3 días");
          anime.setState(false);
        }

        animesProgramming.add(anime);
        index++;
      }
    }

    // Ordenar por fecha y estado: [-1: a primero que b] - [1: b primero que a] -
    // [0: nada]
    animesProgramming.sort((a, b) -> {
      if (a.getDate().equals("Hoy") && b.getDate().equals("Hoy")) {
        if (!a.getState() && b.getState()) {
          return -1;
        } else if (a.getState() && !b.getState()) {
          return 1;
        } else {
          return 0;
        }
      } else if (a.getDate().equals("Hoy") && !b.getDate().equals("Hoy")) {
        return -1;
      } else if (!a.getDate().equals("Hoy") && b.getDate().equals("Hoy")) {
        return 1;
      } else {
        return a.getDate().compareTo(b.getDate());
      }
    });

    return animesProgramming;
  }

  private ChapterDataDTO defineSpecialCases(ChapterDataDTO anime) {
    // Elimina caracteres raros del nombre
    anime.setName(anime.getName().trim().replace("“", String.valueOf('"')).replace("”", String.valueOf('"')));
    anime.setName(this.animeUtils.specialNameOrUrlCases(anime.getName(), 'h')); // Nombres especiales
    anime.setName(anime.getName().replace("Movie", "").trim()); // "Movie" en el nombre
    anime.setUrl(this.animeUtils.specialNameOrUrlCases(anime.getUrl(), 'h')); // Urls especiales

    // Si el número de capítulo tiene un "."
    if (anime.getChapter().contains(".")) {
      int chapter = Integer.parseInt(anime.getChapter().split("\\.")[0]) + 1;

      // Reconstruye la url sin el capítulo
      String[] urlSplit = anime.getUrl().split("-");
      String url = "";
      for (int i = 0; i < urlSplit.length - 1; i++) {
        url += (i != 0) ? "-" + urlSplit[i] : urlSplit[i];
      }

      // Asigna el nuevo capítulo y la nueva url
      anime.setChapter(String.valueOf(chapter));
      anime.setUrl(url + "/" + chapter);

      log.info("---------");
      log.info("chapter: " + anime.getChapter());
      log.info("url: " + anime.getUrl());
      log.info("---------");
    }

    return anime;
  }

  public List<ChapterDataDTO> donghuasProgramming(Document document) {
    Elements elementsJkAnime = document.select(".donghuas_programing a.bloqq");
    List<ChapterDataDTO> lastChapters = new ArrayList<>();

    String year = String.valueOf(LocalDate.now().getYear());
    for (Element item : elementsJkAnime) {
      String date = item.select(".anime__sidebar__comment__item__text span").first().text().trim();

      if (date.equals("Hoy") || date.equals("Ayer")) {
        date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      } else {
        date = DataUtils.parseDate(date + "/" + year, "dd/MM/yyyy", 0);
      }

      ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(item.select(".anime__sidebar__comment__item__text h5").text())
          .imgUrl(item.select(".anime__sidebar__comment__item__pic img").attr("src"))
          .chapter(item.select(".anime__sidebar__comment__item__text h6").text().replace("Episodio", "Capitulo"))
          .type("Donghua")
          .date(this.getFormattedDate(date))
          .url(item.select("a").attr("href").replace(providerJkanimeUrl, ""))
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

      if (!anime.getType().equals("ONA")) {
        latestAddedAnimes.add(anime);
      }
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
    if (dateText.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
      // Manejar el caso de que la fecha sea DIA/MES/AÑO
      LocalDate date = LocalDate.parse((dateText), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      long daysBetween = ChronoUnit.DAYS.between(date, LocalDate.now());

      if (daysBetween <= 7) {
        if (daysBetween == 0) {
          return "Hoy";
        } else if (daysBetween == 1) {
          return "Ayer";
        } else {
          return "Hace " + daysBetween + " días";
        }
      } else {
        String[] dateArray = dateText.split("/");
        return dateArray[0] + "/" + dateArray[1];
      }
    } else {
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

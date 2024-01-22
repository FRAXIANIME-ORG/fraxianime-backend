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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;

@Service
@Log
public class HomePageService {
  @Value("${PROVEEDOR_JKANIME_URL}")
  private String proveedorJkanimeUrl;

  public List<ChapterDataDTO> sliderAnimes(Document document) {
    Elements elements = document.select(".hero__items");
    List<ChapterDataDTO> sliderAnimes = new ArrayList<>();
    
    for (Element element : elements) {
      ChapterDataDTO anime = ChapterDataDTO.builder()
        .name(element.select(".hero__text h2").text())
        .imgUrl(element.attr("data-setbg"))
        .url(element.select(".hero__text a").attr("href").replace(proveedorJkanimeUrl, ""))
        .build();

      sliderAnimes.add(anime);
    }

    return sliderAnimes;
  }

  public List<LastAnimeDataDTO> ovasOnasSpecials(Document document) {
    Elements elements = document.select(".solopc").last().select(".anime__item");
    List<LastAnimeDataDTO> ovasOnasSpecials = new ArrayList<>();

    for (Element element : elements) {
      LastAnimeDataDTO anime = LastAnimeDataDTO.builder()
        .name(element.select(".anime__item__text a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(proveedorJkanimeUrl, ""))
        .type(element.select(".anime__item__text ul li").text())
        .build();

      ovasOnasSpecials.add(anime);
    }

    return ovasOnasSpecials;
  }

  public List<ChapterDataDTO> genericProgramming(Document document, Document docCompare, char type) {
    Elements elements = document.select(".anime_programing a.bloqq");
    List<ChapterDataDTO> lastChapters = new ArrayList<>();
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
          if (daysBetween == 1) {
            formattedDate = "Ayer";
          } else {
            formattedDate = "Hace " + daysBetween + " días";
          }
        } else {
          formattedDate = dateText;
        }
      } else { 
        // Manejar otros casos si los hay
        formattedDate = dateText;
      }

      ChapterDataDTO anime = ChapterDataDTO.builder()
        .name(element.select(".anime__sidebar__comment__item__text h5").text())
        .imgUrl(element.select(".anime__sidebar__comment__item__pic img").attr("src"))
        .chapter(element.select(".anime__sidebar__comment__item__text h6").text().replace("Episodio", "Capitulo"))
        .date(formattedDate)
        .url(element.attr("href").replace(proveedorJkanimeUrl, ""))
        .build();

      lastChapters.add(anime);
    }

    if (type == 'a') {
      lastChapters = this.genericProgrammingConfirmed(lastChapters, docCompare, 'a');
    } else if (type == 'd') {
      lastChapters = this.genericProgrammingConfirmed(lastChapters, docCompare, 'd');
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
      .url(firstTop.select("a").attr("href").replace(proveedorJkanimeUrl, ""))
      .build();
    topAnimes.add(firstAnime);

    Elements restTop = data.child(3).select("a");
    for (Element element : restTop) {
      TopDataDTO anime = TopDataDTO.builder()
        .name(element.select(".comment h5").text())
        .imgUrl(element.select(".anime__item__pic__fila4").attr("data-setbg"))
        .likes(Integer.parseInt(element.select(".vc").text()))
        .position(Integer.parseInt(element.select(".anime__item__pic__fila4 div").first().text().trim()))
        .url(element.attr("href").replace(proveedorJkanimeUrl, ""))
        .build();

      topAnimes.add(anime);
    }

    return topAnimes;
  }

  public List<LastAnimeDataDTO> latestAddedAnimes(Document document) {
    Elements elements = document.select(".trending__anime .anime__item");
    List<LastAnimeDataDTO> latestAddedAnimes = new ArrayList<>();

    for (Element element : elements) {
      LastAnimeDataDTO anime = LastAnimeDataDTO.builder()
        .name(element.select(".anime__item__text h5 a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(proveedorJkanimeUrl, ""))
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
        .url(element.select("a").attr("href").replace(proveedorJkanimeUrl, ""))
        .build();

      latestAddedList.add(anime);
    }

    return latestAddedList;
  }

  public List<ChapterDataDTO> genericProgrammingConfirmed(List<ChapterDataDTO> list, Document docCompare, char type) {
    List<ChapterDataDTO> listToCompare = new ArrayList<>();
    
    log.info("Animes JKAnime:");

    for (ChapterDataDTO anime : list) {
      // anime.setChapter(this.specialChapterCases(anime.getName(), anime.getChapter()));
      log.info(anime.getName() + " - " + anime.getChapter());
    }

    // Compara la lista de animes de JKAnime con la lista de últimos agregados de AnimeLife
    Elements elementsToCompareLastAdded = docCompare.body().select(".excstf").first().select(".bs");
    log.info("Ultimos animes agregados:");

    for (Element element : elementsToCompareLastAdded) {
      ChapterDataDTO anime = ChapterDataDTO.builder()
      .name(element.select(".tt").first().childNodes().stream()
        .filter(node -> !(node instanceof Element && ((Element) node).tag().getName().equals("h2")))
        .map(Node::toString)
        .collect(Collectors.joining()).trim())
      .chapter(element.select(".epx").text().replace("Ep 0", "Capitulo ").replace("Ep ", "Capitulo ").trim())
      .build();

      String typez = element.select(".typez").text().trim();
      if (!typez.equals("TV")) {
        anime.setChapter(typez);
      }

      anime.setName(this.specialNameCases(anime.getName()));
      anime.setChapter(this.specialChapterCases(anime.getName(), anime.getChapter()));
      listToCompare.add(anime);

      log.info("Elemento no disponible de Último animes agregados: " + anime.getName() + " - " + anime.getChapter());
    }

    // Compara la lista de animes de JKAnime con la lista de series en emisión de AnimeLife
    Elements elementsToCompareEmisionSeries = docCompare.body().select(".ongoingseries ul li");
    log.info("Series en emision:");

    for (Element element : elementsToCompareEmisionSeries) {
      ChapterDataDTO anime = ChapterDataDTO.builder()
      .name(element.select(".l").text().trim())
      .chapter(element.select(".r").text().replace("Episodios 0", "Capitulo ").replace("Episodios ", "Capitulo ").trim())
      .build();

      anime.setName(this.specialNameCases(anime.getName()));
      anime.setChapter(this.specialChapterCases(anime.getName(), anime.getChapter()));

      listToCompare.add(anime);

      log.info("Elemento no disponible de Series en emision: " + anime.getName() + " - " + anime.getChapter());
    }

    // Establece el estado de disponibilidad de cada anime de la lista de JKAnime
    // comparando con la lista última de animes agregados y de series en emisión de AnimeLife
    for (ChapterDataDTO item : list) {
      boolean matchFound = listToCompare.stream()
        .anyMatch(compareItem ->
          compareItem.getName().startsWith(item.getName()) &&
          compareItem.getChapter().equals(item.getChapter())
        );
      
      item.setState(matchFound);

      // Si state es false, se elimina el item de la lista
      if (type == 'a' && !matchFound) {
        log.info("Elemento no disponible: " + item.getName() + " - " + item.getChapter());
        // list.remove(item);
      }
    }

    return list;
  }
  
  
  private String specialNameCases(String inputName) {
    Map<String, String> specialCases = new HashMap<>();

    specialCases.put("Solo Leveling", "Ore dake Level Up na Ken");

    for (Map.Entry<String, String> entry : specialCases.entrySet()) {
      if (inputName.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return inputName;
  }

  // Este metodo lo que hace es que si el nombre del anime contiene el nombre de la key del map, entonces
  // se compara el capitulo con el valor de la key del map, si son iguales, entonces se devuelve el valor
  // del map que corresponde a la key del map
  private String specialChapterCases(String inputName, String inputChapter) {
    Map<String, Map<Integer, Integer>> specialCases = new HashMap<>();
    String chapter = inputChapter.replace("Capitulo ", "").trim();

    specialCases.put("Kaitakuki: Around 40 Onsen Mania no Tensei Saki wa, Nonbiri Onsen Tengoku deshita", Map.of(1, 2, 2, 1));

    log.info("---------");

    for (Map.Entry<String, Map<Integer, Integer>> entry : specialCases.entrySet()) {
      if (inputName.contains(entry.getKey())) {
        log.info("1");
        log.info("chapter: " + chapter);
        log.info("entry.getValue().get(1): " + entry.getValue().get(1));
        log.info("entry.getValue().get(2): " + entry.getValue().get(2));
        if (chapter.equals(String.valueOf(entry.getValue().get(1)))) {
          log.info("2");
          return String.valueOf("Capitulo " + entry.getValue().get(2));
        }
      }
    }

    log.info("---------");

    return inputChapter;
  }
}

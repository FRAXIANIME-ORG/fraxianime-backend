package xyz.kiridepapel.fraxianimebackend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.sql.Time;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
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
import xyz.kiridepapel.fraxianimebackend.dto.PageDTO.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;
import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
@Log
public class HomeService {
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_JKANIME_URL}")
  private String providerJkanimeUrl;
  @Value("${PROVIDER_ANIMELIFE_URL}")
  private String providerAnimeLifeUrl;
  @Autowired
  private ScheduleService scheduleService;
  @Autowired
  private DataUtils dataUtils;
  @Autowired
  AnimeUtils animeUtils;

  @Cacheable(value = "home", key = "'animes'")
  public HomePageDTO homePage() {
    Document docAnimesJk = this.dataUtils.simpleConnect(this.providerJkanimeUrl, "Proveedor 1 inactivo");
    Document docScheduleJk = this.dataUtils.simpleConnect(this.providerJkanimeUrl + "horario", "Proveedor 1 inactivo");
    Document docAnimesLf = this.dataUtils.simpleConnect(this.providerAnimeLifeUrl, "Proveedor 2 inactivo");

    // Mapa de casos especiales donde JkAnime se acopla a AnimeLife (normalmente es al revés)
    Map<String, String> mapListTypeJk = new HashMap<>();
    this.scheduleService.getSpecialCases('k').forEach(sce -> mapListTypeJk.put(sce.getOriginal(), sce.getMapped()));

    // Listas de animes programados y próximos animes programados
    List<ChapterDataDTO> animesProgramming = this.animesProgrammingLife(docAnimesLf, docAnimesJk, mapListTypeJk);
    List<ChapterDataDTO> donghuasProgramming = this.donghuasProgramming(docAnimesJk, mapListTypeJk);
    List<ChapterDataDTO> nextAnimesProgramming = this.nextAnimesProgramming(docScheduleJk, donghuasProgramming, mapListTypeJk);

    HomePageDTO animes = HomePageDTO.builder()
        .sliderAnimes(this.sliderAnimes(docAnimesJk))
        .ovasOnasSpecials(this.ovasOnasSpecials(docAnimesJk))
        .animesProgramming(this.changeImgAnimesProgramming(animesProgramming, nextAnimesProgramming))
        .nextAnimesProgramming(this.removeNextAnimesIfWasUploaded(animesProgramming, nextAnimesProgramming))
        .donghuasProgramming(donghuasProgramming)
        .topAnimes(this.topAnimes(docAnimesJk))
        .latestAddedAnimes(this.latestAddedAnimes(docAnimesJk))
        .build();

    animes.setLatestAddedList(this.latestAddedList(docAnimesJk, animes.getLatestAddedAnimes()));
    animes.setLatestAddedAnimes(this.removeOnasFromLatestAddedAnimes(animes.getLatestAddedAnimes()));

    return animes;
  }

  private List<ChapterDataDTO> sliderAnimes(Document document) {
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

  private List<AnimeDataDTO> ovasOnasSpecials(Document document) {
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

  private List<ChapterDataDTO> animesProgrammingLife(Document docAnimesLf, Document docAnimesJk, Map<String, String> mapListTypeJk) {
    // Lista de animes programados en AnimeLife
    Elements elementsAnimeLife = docAnimesLf.body().select(".excstf").first().select(".bs");

    // Mapa de animes programados en JkAnime
    Map<String, ChapterDataDTO> listJkAnime = this.animesProgrammingJkAnime(docAnimesJk, mapListTypeJk);

    // lista de animes en Animelife (utiliza el mapa de los animes de JkAnime para compararlos y usar las fechas y las imagenes de JkAnime)
    List<ChapterDataDTO> listLife = new ArrayList<>();

    // Mapa en base a la combinacion de los casos especiales 's' y 'n' (h) para AnimeLife
    Map<String, String> mapListH = new HashMap<>();
    List<SpecialCaseEntity> homeList = this.scheduleService.getSpecialCases('s');
    homeList.addAll(this.scheduleService.getSpecialCases('n'));
    homeList.forEach(hsce -> mapListH.put(hsce.getOriginal(), hsce.getMapped()));

    // Recorre, guarda y compara los animes de AnimeLife con los de JkAnime
    int index = 0;
    for (Element item : elementsAnimeLife) {
      String url = this.getNameAndChapterFromUrl(item.select(".bsx a").attr("href"));

      if (url.contains("/")) {
        String chapter = item.select(".epx").text().replace("Ep 0", "").replace("Ep ", "").trim();

        if (chapter != null && !chapter.isEmpty()) {
          // * Crea el objeto del anime con los datos obtenidos
          ChapterDataDTO anime = ChapterDataDTO.builder()
              .name(item.select(".tt").first().childNodes().stream()
                  .filter(node -> !(node instanceof Element && ((Element) node).tag().getName().equals("h2")))
                  .map(Node::toString)
                  .collect(Collectors.joining()).trim())
              .imgUrl(item.select("img").attr("src").trim().replace("?resize=247,350", ""))
              .type(item.select(".typez").text().trim().replace("TV", "Anime"))
              .chapter(chapter)
              .url(url)
              .build();
          
          // * Define los casos especiales de nombres y URLs
          anime = this.defineSpecialCases(mapListH, anime, 'h');
          
          // * Toma la imagen y la fecha de JkAnime si es que el anime está en JkAnime, si no,
          // * asigna una fecha a partir de la posición en la lista de animes de AnimeLife
          if (listJkAnime.containsKey(anime.getName())) {
            // Si el anime está en Jkanime, usa la fecha y la imagen de Jkanime
            anime.setImgUrl(listJkAnime.get(anime.getName()).getUrl());
            anime.setDate(this.getPastFormattedDate(listJkAnime.get(anime.getName()).getDate()));
            anime.setState(true);
          } else {
            // Si el anime no está en Jkanime, asigna una fecha a partir de la posición en la lista de animes de AnimeLife
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
  
          listLife.add(anime);
        }

        index++;
      }
    }

    return this.sortAnimeList(listLife, true);
  }

  private ChapterDataDTO defineSpecialCases(Map<String, String> mapListH, ChapterDataDTO anime, char type) {
    // Elimina caracteres raros del nombre
    anime.setName(anime.getName().trim().replace("“", String.valueOf('"')).replace("”", String.valueOf('"')));
    // Nombres de casos especiales
    anime.setName(this.animeUtils.specialNameOrUrlCases(mapListH, anime.getName(), type, "animesProgrammingLife()"));
    // Elimina "Movie" del nombre
    anime.setName(anime.getName().replace("Movie", "").trim());
    // URLs de casos especiales
    anime.setUrl(this.animeUtils.specialNameOrUrlCases(mapListH, anime.getUrl(), type, "animesProgrammingLife()"));
    
    // log.info("X. " + anime.getName() + " - " + anime.getChapter() + " (" + anime.getUrl() + ")");

    return anime;
  }

  private Map<String, ChapterDataDTO> animesProgrammingJkAnime(Document docAnimesJk, Map<String, String> mapListTypeJk) {
    // Lista de animes programados en JkAnime
    Elements elementsJkAnime = docAnimesJk.body().select(".listadoanime-home .anime_programing a");
    // Mapa para guardar los animes de JkAnime (key: Nombre, Value: Datos del anime)
    Map<String, ChapterDataDTO> listJkAnime = new HashMap<>();

    // Fecha exacta con tiempo UTC y 5 horas menos si esta en produccion (Hora de Perú)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES"));
    Date todayD = DataUtils.getDateNow(isProduction);
    LocalDate todayLD = DataUtils.getLocalDateTimeNow(isProduction).toLocalDate();
    String year = String.valueOf(DataUtils.getLocalDateTimeNow(isProduction).getYear());
    Calendar nowCal = Calendar.getInstance();
    nowCal.setTime(todayD);
    
    // Guarda en un mapa los animes de JkAnime (clave: nombre del anime, valor: datos del anime)
    for (Element item : elementsJkAnime) {
      String name = item.select("h5").first().text().trim();
      String url = item.select(".anime__sidebar__comment__item__pic img").attr("src").trim();
      String date = item.select(".anime__sidebar__comment__item__text span").first().text().trim();

      // Todas las fechas se guardan en el formato dd/MM/yyyy y más adelante se formatean a "Hoy", "Ayer", "Hace x días" o dd/MM
      if (date.equals("Hoy") || date.equals("Ayer")) {
        // Si es Hoy o Ayer pero actualmente es entre las 19:00 y 23:59, entonces es "Hoy" en foramto dd/MM/yyyy
        if (date.equals("Hoy") || (date.equals("Ayer") && nowCal.get(Calendar.HOUR_OF_DAY) >= 19 && nowCal.get(Calendar.HOUR_OF_DAY) <= 23))
          date = todayLD.format(formatter);
        // Si es Ayer pero actualmente es entre las 00:00 y 18:59, entonces es "Ayer" en formato dd/MM/yyyy
        else date = todayLD.minusDays(1).format(formatter);
      }
      // Si no es Hoy ni Ayer, entonces es una fecha en formato dd/MM/yyyy
      else date = DataUtils.parseDate(date + "/" + year, formatter, 0);

      ChapterDataDTO data = ChapterDataDTO.builder()
          .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "animesProgrammingJkAnime()"))
          .date(date)
          .url(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, url, 'k', "animesProgrammingJkAnime()"))
          .build();
      
      listJkAnime.put(data.getName(), data);
    }

    return listJkAnime;
  }

  private List<ChapterDataDTO> nextAnimesProgramming(Document document, List<ChapterDataDTO> donghuasProgramming, Map<String, String> mapListTypeJk) {
    // Elimina el ultimo elemento (filtro)
    Elements elements = document.select(".box.semana");
    elements.remove(elements.size() - 1);
    List<ChapterDataDTO> nextAnimesProgramming = new ArrayList<>();

    // Obtener el indice del dia actual
    Integer startIndex = -1;
    outerloop: for (Element item : elements) {
      Elements animesDay = item.select(".cajas .box");
      for (Element subItem : animesDay) {
        String date = this.parseAndSetNextChapterDateSchedule(subItem.select(".last time").text().split(" ")[0]);

        // Verificar si la fecha es hoy
        LocalDate todayLD = DataUtils.getLocalDateTimeNow(isProduction).toLocalDate();
        boolean isToday = date.equals(todayLD.format(DateTimeFormatter.ofPattern("dd/MM")));

        if (isToday) {
          startIndex = elements.indexOf(item);
          break outerloop;
        }
      }
    }

    // Recorre los animes emitidos en el apartado 'horario' de JkAnime
    List<ChapterDataDTO> tempLastAnimes = new ArrayList<>();
    int index = 0;
    for (Element item : elements) {
      // String day = DataUtils.removeDiacritics(item.select("h2").text());
      Elements animesDay = item.select(".cajas .box");

      for (Element subItem : animesDay) {
        String name = subItem.select("a").first().select("h3").text();
        String url = subItem.select("a").first().attr("href").replace("/", "");
        String chapter = subItem.select(".last span").text().split(":")[1].trim();

        String[] dateWithTime = subItem.select(".last time").text().split(" ");
        String date = this.formattedNextDate(name, dateWithTime[0]);
        String timeStr = dateWithTime[1];
        
        // Corregir la hora si es mayor a las 20:00
        String[] listBadTimes = { "20", "21", "22", "23" };
        if (List.of(listBadTimes).contains(timeStr.substring(0, 2))) {
          Time time = Time.valueOf(timeStr);
          time.setTime(time.getTime() - 28800000); // -8 Horas
          timeStr = time.toString();
        }

        ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "nextAnimesProgramming()"))
          .imgUrl(subItem.select(".boxx img").attr("src"))
          .type("Anime")
          .chapter(String.valueOf(Integer.parseInt(chapter) + 1))
          .date(date)
          .time(timeStr.substring(0, timeStr.length() - 3))
          .url(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, url, 'k', "nextAnimesProgramming()"))
          .build();
        
        if (index < startIndex) {
          tempLastAnimes.add(anime);
        } else {
          nextAnimesProgramming.add(anime);
        }
      }

      index++;
    }

    nextAnimesProgramming.addAll(tempLastAnimes);

    return this.sortAnimeList(nextAnimesProgramming, false);
  }

  // private Map<String, ChapterDataDTO> add

  // Cambia las URLs de imagen de animesProgramming por las URLs de imagen de nextAnimesProgramming
  private List<ChapterDataDTO> changeImgAnimesProgramming(List<ChapterDataDTO> animesProgramming, List<ChapterDataDTO> nextAnimesProgramming) {
    Map<String, String> imgUrlsMap = new HashMap<>();

    // Llenar el mapa con los nombres de los animes y sus respectivas URLs de imagen de nextAnimesProgramming
    for (ChapterDataDTO nextAnime : nextAnimesProgramming) {
      imgUrlsMap.put(nextAnime.getName(), nextAnime.getImgUrl());
    }

    // Actualizar las URLs de imagen en animesProgramming si se encuentra una coincidencia en el mapa
    for (ChapterDataDTO anime : animesProgramming) {
      String imgUrl = imgUrlsMap.get(anime.getName());
      if (imgUrl != null) {
        anime.setImgUrl(imgUrl);
      }
    }

    return animesProgramming;
  }
  
  // Elimina los animes que ya fueron subidos en la lista de animes programados
  private List<ChapterDataDTO> removeNextAnimesIfWasUploaded(List<ChapterDataDTO> animes, List<ChapterDataDTO> nextAnimes) {
    List<ChapterDataDTO> nextAnimesCopy = new ArrayList<>(nextAnimes);

    for (ChapterDataDTO nextAnime : nextAnimes) {
      try {
        // Si coincide el nombre y el capítulo del anime de "Hoy" con el anime de "Hoy" o "Ayer" en subidos
        Integer pastChapter = Integer.parseInt(nextAnime.getChapter()) - 1;
        if (animes.stream().anyMatch(aP ->
            // Si esta en subidos "Hoy" y en programados "Hoy"
            aP.getName().equals(nextAnime.getName()) &&
            // Si el capítulo de "Hoy" es igual al capítulo en programados o al capítulo anterior
            (aP.getChapter().equals(nextAnime.getChapter()) || aP.getChapter().equals(pastChapter.toString())) &&
            aP.getDate().equals("Hoy") ||
            // Si esta en subidos "Ayer" y en programados "Hoy" (ya fue subido ayer, por lo que es un bug)
            aP.getName().equals(nextAnime.getName()) &&
            aP.getChapter().equals(nextAnime.getChapter()) &&
            aP.getDate().equals("Ayer") && nextAnime.getDate().equals("Hoy")
          )
        ) {
          nextAnimesCopy.remove(nextAnime);
          // log.info("Se elimino el anime " + nextAnime.getName() + " de la lista de próximos animes programados");
        }
      } catch (NumberFormatException e) {
        // Si el capítulo no es un número, prueba sin la validación de capítulo
        log.warning("Error al convertir el capitulo: " + nextAnime.getChapter() + " del anime: '" + nextAnime.getName() + "'. Probando sin validación de capítulo.");
        if (animes.stream().anyMatch(aP ->
            aP.getName().equals(nextAnime.getName()) &&
            aP.getDate().equals("Hoy") ||
            aP.getName().equals(nextAnime.getName()) &&
            aP.getDate().equals("Ayer") && nextAnime.getDate().equals("Hoy")
          )
        ) {
          log.warning("Se elimino '" + nextAnime.getName() + "' de la lista de próximos animes programados sin validar el capítulo.");
          nextAnimesCopy.remove(nextAnime); 
        }
      }
    }

    nextAnimesCopy.sort((a, b) -> {
      int priorityA = getPriority(a.getDate());
      int priorityB = getPriority(b.getDate());
      return Integer.compare(priorityA, priorityB);
    });

    return nextAnimesCopy;
  }
  private int getPriority(String date) {
    // Función para ordenar las fechas formateadas de los próximos capítulos que serán subidos

    if (date.equals("Hoy")) return 1;
    if (date.equals("Mañana")) return 2;
    if (date.startsWith("En ")) return 3;
    return 4; // Es dd/MM
  }

  // 
  private String formattedNextDate(String name, String recivedDate) {
    String date = this.calcDaysToNextChapter(name, recivedDate);
    date = this.getNextFormattedDate(date);
    return date;
  }
  
  // 
  private List<ChapterDataDTO> sortAnimeList(List<ChapterDataDTO> animeList, boolean validateStateToo) {
    animeList.sort((a, b) -> {
      if (a.getDate().equals("Hoy") && b.getDate().equals("Hoy")) {
        if (validateStateToo) {
          if (!a.getState() && b.getState()) {
            return -1;
          } else if (a.getState() && !b.getState()) {
            return 1;
          } else {
            return 0;
          }
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

    return animeList;
  }

  // 
  private List<ChapterDataDTO> donghuasProgramming(Document document, Map<String, String> mapListTypeJk) {
    Elements elementsJkAnime = document.select(".donghuas_programing a.bloqq");
    List<ChapterDataDTO> lastChapters = new ArrayList<>();

    String year = String.valueOf(LocalDate.now().getYear());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "ES"));
    for (Element item : elementsJkAnime) {
      String name = item.select(".anime__sidebar__comment__item__text h5").text();
      String url = item.select("a").attr("href").replace(providerJkanimeUrl, "");
      String date = item.select(".anime__sidebar__comment__item__text span").first().text().trim();

      date = date.equals("Hoy") || date.equals("Ayer")
        ? DataUtils.getLocalDateTimeNow(isProduction).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        : DataUtils.parseDate(date + "/" + year, formatter, 0);

      ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "donghuasProgramming()"))
          .imgUrl(item.select(".anime__sidebar__comment__item__pic img").attr("src"))
          .chapter(item.select(".anime__sidebar__comment__item__text h6").text().replace("Episodio", "Capitulo"))
          .type("Donghua")
          .date(this.getPastFormattedDate(date))
          .url(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, url, 'k', "donghuasProgramming()"))
          .state(true)
          .build();

      // Quitar el "/" final de la url
      if (anime.getUrl().endsWith("/"))
        anime.setUrl(anime.getUrl().substring(0, anime.getUrl().length() - 1));

      lastChapters.add(anime);
    }

    return lastChapters;
  }

  // 
  private List<TopDataDTO> topAnimes(Document document) {
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

  // 
  private List<AnimeDataDTO> latestAddedAnimes(Document document) {
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
  
  // 
  private List<AnimeDataDTO> removeOnasFromLatestAddedAnimes(List<AnimeDataDTO> latestAddedAnimes) {
    List<AnimeDataDTO> listWithoutOnas = new ArrayList<>();

    for (AnimeDataDTO anime : latestAddedAnimes) {
      if (!anime.getType().equals("ONA")) {
        listWithoutOnas.add(anime);
      }
    }

    return listWithoutOnas;
  }

  // 
  private List<LinkDTO> latestAddedList(Document document, List<AnimeDataDTO> latestAddedAnimes) {
    Elements elements = document.select(".trending_div .side-menu li");
    List<LinkDTO> latestAddedList = new ArrayList<>();
    Map<String, String> mapOnasNames = new HashMap<>();

    // Llenar el mapa con los nombres de los ONAs
    latestAddedAnimes.stream().filter(a -> a.getType().equals("ONA")).forEach(a -> {
      mapOnasNames.put(a.getName(), "f");
    });

    for (Element element : elements) {
      String name = element.select("a").text();
      
      // Si no está en la lista de ONAS, es un anime y lo agrega a la lista
      if (mapOnasNames.get(name) == null) {
        // Si es un anime
        LinkDTO anime = LinkDTO.builder()
          .name(name)
          .url(element.select("a").attr("href").replace(providerJkanimeUrl, ""))
          .build();
  
        latestAddedList.add(anime);
      }
    }

    return latestAddedList;
  }

  // ? Text
  // * Obtiene el nombre junto al capítulo del anime a partir de la URL (one-piece-1090 -> [one-piece/1090])
  private String getNameAndChapterFromUrl(String url) {
    String newUrl = url.replace(this.providerAnimeLifeUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");

    // Verifica si la URL termina con el patrón -xx-2
    if (url.matches(".*-\\d{2}-2/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(\\d{2})-2/?$", "$1");
      try {
        int number = Integer.parseInt(numberPart) + 1;
        newUrl = url.replaceFirst("-\\d{2}-2/?$", "/" + number).replace(this.providerAnimeLifeUrl, "");
        // log.info("1: " + url + " -> " + newUrl);
        return newUrl;
      } catch (NumberFormatException e) {
        log.warning("Error parsing number from URL: " + url);
      }
    } else if (url.matches(".*-\\d{1,2}-5/?$")) {
      String numberPart = url.replaceAll("^.*-(0*)(\\d{1,2})-5/?$", "$2");
      newUrl = url.replaceFirst("-\\d{1,2}-5/?$", "/" + numberPart + "-5").replace(this.providerAnimeLifeUrl, "");
      // log.info("2: " + url + " -> " + newUrl);
      return newUrl;
    } else {
      // Si no termina con -xx-2, solo elimina los ceros a la izquierda
      newUrl = url.replace(this.providerAnimeLifeUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
      // log.info("3: " + url + " -> " + newUrl);
      return newUrl;
    }

    // Verifica si la URL termina con el patrón -xx-2
    if (url.matches(".*-\\d{2}-2/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(\\d{2})-2/?$", "$1");
      try {
        int number = Integer.parseInt(numberPart) + 1;
        newUrl = url.replaceFirst("-\\d{2}-2/?$", "/" + number);
        return newUrl.replace(this.providerAnimeLifeUrl, "");
      } catch (NumberFormatException e) {
        log.warning("Error parsing number from URL: " + url);
      }
    } else {
      // Si no termina con -xx-2, solo elimina los ceros a la izquierda
      newUrl = url.replace(this.providerAnimeLifeUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
      return newUrl;
    }



    return newUrl;
  }

  // ? Dates
  // Formatea la fecha en la que se publicaron los capítulos
  private String getPastFormattedDate(String dateText) {
    if (dateText.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
      // Manejar el caso de que la fecha sea DIA/MES/AÑO
      LocalDate todayLD = DataUtils.getLocalDateTimeNow(isProduction).toLocalDate();
      LocalDate date = LocalDate.parse((dateText), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      long daysBetween = ChronoUnit.DAYS.between(date, todayLD);
      
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
  
  // Formatea la fecha en la que se publicarán los siguientes capítulos
  private String getNextFormattedDate(String dateText) {
    if (dateText.matches("^\\d{1,2}/\\d{1,2}$")) {
      // Manejar el caso de que la fecha sea DIA/MES/AÑO
      LocalDate todayLD = DataUtils.getLocalDateTimeNow(isProduction).toLocalDate();
      LocalDate date = LocalDate.parse((dateText + "/" + todayLD.getYear()), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      long daysBetween = ChronoUnit.DAYS.between(todayLD, date);

      if (daysBetween <= 7) {
        if (daysBetween == 0) {
          return "Hoy";
        } else if (daysBetween == 1) {
          return "Mañana";
        } else {
          return "En " + (1 * daysBetween) + " días";
        }
      } else {
        String[] dateArray = dateText.split("/");
        return dateArray[0] + "/" + dateArray[1];
      }
    } else {
      return dateText;
    }
  }
  
  // Calcula los días que faltan para el siguiente capítulo en base
  // a la fecha de publicación y a la fecha actual
  private String calcDaysToNextChapter(String name, String chapterDate) {
    DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd", new Locale("es", "ES"));
    DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("dd/MM", new Locale("es", "ES"));

    LocalDate todayLDT = DataUtils.getLocalDateTimeNow(this.isProduction).toLocalDate();
    
    LocalDate date = LocalDate.parse(chapterDate, formatterInput);
    DayOfWeek weekDay = date.getDayOfWeek();

    int todayValue = todayLDT.getDayOfWeek().getValue();
    int dateValue = weekDay.getValue();
    
    int daysUntilDate = Math.abs(dateValue - todayValue);

    // Si hoy es un día posterior al date, cuenta los días hasta el próximo date en la semana siguiente
    if (todayValue > dateValue) {
      daysUntilDate = 7 - daysUntilDate;
    }

    return todayLDT.plusDays(daysUntilDate).format(formatterOutput);
  }

  // Establece la fecha del siguiente capítulo en base a la fecha del último capítulo
  private String parseAndSetNextChapterDateSchedule(String recivedDate) {
    DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd", new Locale("es", "ES"));
    DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("dd/MM", new Locale("es", "ES"));

    LocalDate date = LocalDate.parse(recivedDate, formatterInput);

    String finalDate = date.plusDays(7).format(formatterOutput);

    return finalDate;
  }
}

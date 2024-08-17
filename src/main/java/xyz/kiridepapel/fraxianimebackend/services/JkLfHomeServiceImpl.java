package xyz.kiridepapel.fraxianimebackend.services;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.TopDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.HomePageDTO;
import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.interfaces.IJkLfHomeService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
@Log
public class JkLfHomeServiceImpl implements IJkLfHomeService {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_1}")
  private String provider1;
  @Value("${PROVIDER_2}")
  private String provider2;
  // Inyección de dependencias
  private CacheUtils cacheUtils;
  private DataUtils dataUtils;
  private AnimeUtils animeUtils;

  // Constructor
  public JkLfHomeServiceImpl(CacheUtils cacheUtils, DataUtils dataUtils, AnimeUtils animeUtils) {
    this.cacheUtils = cacheUtils;
    this.dataUtils = dataUtils;
    this.animeUtils = animeUtils;
  }

  @Cacheable(value = "home", key = "'animes'")
  public HomePageDTO home() {
    Document docAnimesPv1 = this.dataUtils.simpleConnect(this.provider1, "Proveedor 1 inactivo");
    // Document docSchedulePv1 = this.dataUtils.simpleConnect(this.provider1 + "horario", "Proveedor 1 inactivo");
    // Document docAnimesPv2 = this.dataUtils.simpleConnect(this.provider2, "Proveedor 2 inactivo");
    
    // Mapa de casos especiales donde JkAnime se acopla a AnimeLife (normalmente es al revés)
    Map<String, String> mapListTypeJk = new HashMap<>();
    this.cacheUtils.getSpecialCases('k').forEach(sce -> mapListTypeJk.put(sce.getOriginal(), sce.getMapped()));

    // Listas de animes programados y próximos animes programados
    // List<ChapterDataDTO> animesProgrammingPv2 = this.animesProgrammingPv2(docAnimesPv2, docAnimesPv1, mapListTypeJk);
    List<ChapterDataDTO> donghuasProgrammingPv1 = this.donghuasProgrammingPv1(docAnimesPv1, mapListTypeJk);
    // List<ChapterDataDTO> nextAnimesProgrammingPv1 = this.nextAnimesProgrammingPv1(docSchedulePv1, mapListTypeJk);

    HomePageDTO animes = HomePageDTO.builder()
        .sliderAnimes(this.sliderAnimes(docAnimesPv1))
        .ovasOnasSpecials(this.ovasOnasSpecials(docAnimesPv1))
        // .animesProgramming(this.changeImagesInAnimesProgramming(animesProgrammingPv2, nextAnimesProgrammingPv1))
        // .nextAnimesProgramming(this.removeNextAnimesProgrammingIfWasUploaded(animesProgrammingPv2, nextAnimesProgrammingPv1))
        .donghuasProgramming(donghuasProgrammingPv1)
        .topAnimes(this.topAnimes(docAnimesPv1))
        .latestAddedAnimes(this.latestAddedAnimes(docAnimesPv1))
        .build();

    animes.setLatestAddedList(this.latestAddedList(docAnimesPv1, animes.getLatestAddedAnimes()));
    animes.setLatestAddedAnimes(this.removeOnasFromLatestAddedAnimes(animes.getLatestAddedAnimes()));

    return animes;
  }

  // JkAnime: Slider principal de animes
  private List<ChapterDataDTO> sliderAnimes(Document document) {
    Elements elements = document.select(".hero__items");
    List<ChapterDataDTO> sliderAnimes = new ArrayList<>();

    for (Element element : elements) {

      ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(element.select(".hero__text h2").text())
          .imgUrl(element.attr("data-setbg"))
          .url(element.select(".hero__text a").attr("href").replace(provider1, "").replaceFirst("/$", ""))
          .build();

      String[] urlSplit = anime.getUrl().split("/");
      anime.setChapter(urlSplit[urlSplit.length - 1]);

      sliderAnimes.add(anime);
    }

    return sliderAnimes;
  }

  // JkAnime: Ovas, Onas y Especiales
  private List<AnimeDataDTO> ovasOnasSpecials(Document document) {
    Elements elements = document.select(".solopc").last().select(".anime__item");
    List<AnimeDataDTO> ovasOnasSpecials = new ArrayList<>();

    for (Element element : elements) {
      AnimeDataDTO anime = AnimeDataDTO.builder()
          .name(element.select(".anime__item__text a").text())
          .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
          .url(element.select("a").attr("href").replace(provider1, "").split("/")[0].trim())
          .type(element.select(".anime__item__text ul li").text())
          .build();

      ovasOnasSpecials.add(anime);
    }

    return ovasOnasSpecials;
  }

  // AnimeLife: Animes programados
  private List<ChapterDataDTO> animesProgrammingPv2(Document docAnimesPv2, Document docAnimesPv1, Map<String, String> mapListTypeJk) {
    // Lista de animes programados en AnimeLife
    // Elements elementsAnimeLife = docAnimesPv2.body().select(".excstf").first().select(".bs");
    Element contenedor = docAnimesPv2.body().getElementById("sub-latest-releases");

    if (contenedor == null) {
      log.warning("No se encontraron animes programados en el Proveedor 2");
      return new ArrayList<>();
    }
    
    Elements divsConTooltip = contenedor.select("div[data-tooltip-id]");

    log.info("Cantidad de animes programados: " + divsConTooltip.size());

    // Mapa de animes programados en JkAnime
    Map<String, ChapterDataDTO> listAnimesPv1 = this.jkAnimesProgramming(docAnimesPv1, mapListTypeJk);

    // lista de animes en Animelife (utiliza el mapa de los animes de JkAnime para compararlos y usar las fechas y las imagenes de JkAnime)
    List<ChapterDataDTO> listAnimesPv2 = new ArrayList<>();

    // // Mapa en base a la combinacion de los casos especiales 's' y 'n' (h) para AnimeLife
    // Map<String, String> mapListTypeLf = new HashMap<>();
    // List<SpecialCaseEntity> homeList = this.cacheUtils.getSpecialCases('s');
    // homeList.addAll(this.cacheUtils.getSpecialCases('n'));
    // homeList.forEach(hsce -> mapListTypeLf.put(hsce.getOriginal(), hsce.getMapped()));

    // // Recorre, guarda y compara los animes de AnimeLife con los de JkAnime
    // int index = 0;
    // for (Element item : elementsAnimeLife) {
    //   String url = getNameAndChapterFromUrl(this.provider2, item.select(".bsx a").attr("href"));

    //   if (url.contains("/")) {
    //     String chapter = item.select(".epx").text().replace("Ep 0", "").replace("Ep ", "").trim();

    //     if (chapter != null && !chapter.isEmpty()) {
    //       // * Crea el objeto del anime con los datos obtenidos
    //       ChapterDataDTO anime = ChapterDataDTO.builder()
    //           .name(item.select(".tt").first().childNodes().stream()
    //               .filter(node -> !(node instanceof Element && ((Element) node).tag().getName().equals("h2")))
    //               .map(Node::toString)
    //               .collect(Collectors.joining()).trim())
    //           .imgUrl(item.select("img").attr("src").trim().replace("?resize=247,350", ""))
    //           .type(item.select(".typez").text().trim().replace("TV", "Anime"))
    //           .chapter(chapter)
    //           .url(url)
    //           .build();
          
    //       // * Define los casos especiales de nombres y URLs
    //       anime = this.defineSpecialCases(mapListTypeLf, anime, 'H');
          
    //       // * Toma la imagen y la fecha de JkAnime si es que el anime está en JkAnime, si no,
    //       // * asigna una fecha a partir de la posición en la lista de animes de AnimeLife
    //       if (listAnimesPv1.containsKey(anime.getName())) {
    //         // Si el anime está en Jkanime, usa la fecha y la imagen de Jkanime
    //         anime.setImgUrl(listAnimesPv1.get(anime.getName()).getUrl());
    //         anime.setDate(this.formatPastChapterDate(listAnimesPv1.get(anime.getName()).getDate()));
    //         anime.setState(true);
    //       } else {
    //         // Si el anime no está en Jkanime, asigna una fecha a partir de la posición en la lista de animes de AnimeLife
    //         if (index <= 10)
    //           anime.setDate("Hoy");
    //         else if (index <= 15)
    //           anime.setDate("Ayer");
    //         else if (index <= 20)
    //           anime.setDate("Hace 2 días");
    //         else if (index <= 25)
    //           anime.setDate("Hace 3 días");
    //         anime.setState(false);
    //       }
  
    //       listAnimesPv2.add(anime);
    //     }

    //     index++;
    //   }
    // }

    return sortAnimeList(listAnimesPv2, true);
  }

  // Define los casos especiales de los nombres y las URLs de 'Animes programados' de AnimeLife
  private ChapterDataDTO defineSpecialCases(Map<String, String> mapListH, ChapterDataDTO anime, char type) {
    // Elimina caracteres raros del nombre
    anime.setName(AnimeUtils.removeRareCharactersFromName(anime.getName()));
    // Nombres de casos especiales
    anime.setName(this.animeUtils.specialNameOrUrlCases(mapListH, anime.getName(), type, "animesProgrammingPv2()"));
    // Elimina "Movie" del nombre
    anime.setName(anime.getName().replace("Movie", "").trim());
    // URLs de casos especiales
    anime.setUrl(this.animeUtils.specialNameOrUrlCases(mapListH, anime.getUrl(), type, "animesProgrammingPv2()"));

    return anime;
  }

  // JkAnime: Animes programados
  private Map<String, ChapterDataDTO> jkAnimesProgramming(Document docAnimesPv1, Map<String, String> mapListTypeJk) {
    // Lista de animes programados en JkAnime
    Elements elementsJkAnime = docAnimesPv1.body().select(".listadoanime-home .anime_programing a");
    // Mapa para guardar los animes de JkAnime (key: Nombre, Value: Datos del anime)
    Map<String, ChapterDataDTO> listAnimesPv1 = new HashMap<>();

    // Fecha exacta con tiempo UTC y 5 horas menos si esta en produccion (Hora de Perú)
    String format = "dd/MM/yyyy";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, new Locale("es", "ES"));
    // Dia actual
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
      else date = DataUtils.parseDate(date + "/" + year, format, format, 0);

      ChapterDataDTO data = ChapterDataDTO.builder()
          .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "jkAnimesProgramming()"))
          .date(date)
          .url(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, url, 'k', "jkAnimesProgramming()"))
          .build();
      
      listAnimesPv1.put(data.getName(), data);
    }

    return listAnimesPv1;
  }

  // JkAnime: Próximos animes programados
  private List<ChapterDataDTO> nextAnimesProgrammingPv1(Document docAnimesPv1, Map<String, String> mapListTypeJk) {
    // Elimina el ultimo elemento (filtro)
    Elements elements = docAnimesPv1.select(".box.semana");
    elements.remove(elements.size() - 1);
    List<ChapterDataDTO> nextAnimesProgrammingPv1 = new ArrayList<>();

    // Obtener el indice del dia actual
    Integer startIndex = -1;
    outerloop: for (Element item : elements) {
      Elements animesDay = item.select(".cajas .box");
      for (Element subItem : animesDay) {
        String time = subItem.select(".last time").text().split(" ")[0];
        String date = DataUtils.parseDate(time, "yyyy-MM-dd", "dd/MM", 7);

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
        String date = this.nextChapterDate(name, dateWithTime[0]);
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
          nextAnimesProgrammingPv1.add(anime);
        }
      }

      index++;
    }

    nextAnimesProgrammingPv1.addAll(tempLastAnimes);

    return sortAnimeList(nextAnimesProgrammingPv1, false);
  }

  // Cambia las imagenes de 'Animes programados' por las imagenes de 'Próximos animes programados'
  private List<ChapterDataDTO> changeImagesInAnimesProgramming(List<ChapterDataDTO> animes, List<ChapterDataDTO> nextAnimes) {
    Map<String, String> imgUrlsMap = new HashMap<>();

    // Llenar el mapa con los nombres de los animes y sus respectivas URLs de imagen de nextAnimesProgramming
    for (ChapterDataDTO nextAnime : nextAnimes) {
      imgUrlsMap.put(nextAnime.getName(), nextAnime.getImgUrl());
    }

    // Actualizar las URLs de imagen en animesProgramming si se encuentra una coincidencia en el mapa
    for (ChapterDataDTO anime : animes) {
      String imgUrl = imgUrlsMap.get(anime.getName());
      if (imgUrl != null) {
        anime.setImgUrl(imgUrl);
      }
    }

    return animes;
  }
  
  // Elimina los animes de 'Próximos animes programados' que ya están en 'Animes programados' (ya fueron subidos)
  private List<ChapterDataDTO> removeNextAnimesProgrammingIfWasUploaded(List<ChapterDataDTO> animes, List<ChapterDataDTO> nextAnimes) {
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

  // Función auxiliar para ordenar las fechas formateadas en la que serán subidos los animes de 'Próximos animes programados'
  private Integer getPriority(String date) {
    if (date.equals("Hoy")) return 1;
    if (date.equals("Mañana")) return 2;
    if (date.startsWith("En ")) return 3;
    return 4; // Es dd/MM
  }

  // JkAnime: Donghuas programados
  private List<ChapterDataDTO> donghuasProgrammingPv1(Document document, Map<String, String> mapListTypeJk) {
    Elements elementsJkAnime = document.select(".donghuas_programing a.bloqq");
    List<ChapterDataDTO> lastChapters = new ArrayList<>();

    String year = String.valueOf(LocalDate.now().getYear());
    for (Element item : elementsJkAnime) {
      String name = item.select(".anime__sidebar__comment__item__text h5").text();
      String url = item.select("a").attr("href").replace(provider1, "");
      String date = item.select(".anime__sidebar__comment__item__text span").first().text().trim();

      date = date.equals("Hoy") || date.equals("Ayer")
        ? DataUtils.getLocalDateTimeNow(isProduction).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        : DataUtils.parseDate(date + "/" + year, "dd/MM/yyyy", "dd/MM/yyyy", 0);

      ChapterDataDTO anime = ChapterDataDTO.builder()
          .name(this.animeUtils.specialNameOrUrlCases(mapListTypeJk, name, 'k', "donghuasProgramming()"))
          .imgUrl(item.select(".anime__sidebar__comment__item__pic img").attr("src"))
          .chapter(item.select(".anime__sidebar__comment__item__text h6").text().replace("Episodio", "Capitulo"))
          .type("Donghua")
          .date(this.formatPastChapterDate(date))
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

  // JkAnime: Top 10 animes más votados
  private List<TopDataDTO> topAnimes(Document document) {
    Element data = document.select(".destacados").last().select(".container div").first();
    List<TopDataDTO> topAnimes = new ArrayList<>(10);

    Element firstTop = data.child(2);
    TopDataDTO firstAnime = TopDataDTO.builder()
        .name(firstTop.select(".comment h5").text())
        .imgUrl(firstTop.select(".anime__item__pic").attr("data-setbg"))
        .likes(Integer.parseInt(firstTop.select(".vc").text().trim()))
        .position(Integer.parseInt(firstTop.select(".ep").text().trim()))
        .url(firstTop.select("a").attr("href").replace(provider1, "").replaceFirst("/$", ""))
        .build();
    topAnimes.add(firstAnime);

    Elements restTop = data.child(3).select("a");
    for (Element element : restTop) {
      TopDataDTO anime = TopDataDTO.builder()
          .name(element.select(".comment h5").text())
          .imgUrl(element.select(".anime__item__pic__fila4").attr("data-setbg"))
          .likes(Integer.parseInt(element.select(".vc").text()))
          .position(Integer.parseInt(element.select(".anime__item__pic__fila4 div").first().text().trim().replaceFirst("/$", "")))
          .url(element.attr("href").replace(provider1, ""))
          .build();

      topAnimes.add(anime);
    }

    return topAnimes;
  }

  // JkAnime: Últimos animes agregados
  private List<AnimeDataDTO> latestAddedAnimes(Document document) {
    Elements elements = document.select(".trending__anime .anime__item");
    List<AnimeDataDTO> latestAddedAnimes = new ArrayList<>();

    for (Element element : elements) {
      AnimeDataDTO anime = AnimeDataDTO.builder()
        .name(element.select(".anime__item__text h5 a").text())
        .imgUrl(element.select(".anime__item__pic").attr("data-setbg"))
        .url(element.select("a").attr("href").replace(provider1, "").replaceFirst("/$", ""))
        .state(element.select(".anime__item__text ul li").first().text().replace("Por estrenar", "Proximamente"))
        .type(element.select(".anime__item__text ul li").last().text())
        .build();

      latestAddedAnimes.add(anime);
    }

    return latestAddedAnimes;
  }
  
  // Elimina los ONAs de la lista de 'Últimos animes agregados'
  private List<AnimeDataDTO> removeOnasFromLatestAddedAnimes(List<AnimeDataDTO> latestAddedAnimes) {
    List<AnimeDataDTO> listWithoutOnas = new ArrayList<>();

    for (AnimeDataDTO anime : latestAddedAnimes) {
      if (!anime.getType().equals("ONA")) {
        listWithoutOnas.add(anime);
      }
    }

    return listWithoutOnas;
  }

  // JkAnime: Lista de los últimos animes agregados
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
          .url(element.select("a").attr("href").replace(provider1, "").replaceFirst("/$", ""))
          .build();
  
        latestAddedList.add(anime);
      }
    }

    return latestAddedList;
  }

  // ? Funciones
  // Ordena una lista en base a la fecha y/o a si el anime ya fue subido
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

  // one-piece-1090 -> one-piece/1090
  private String getNameAndChapterFromUrl(String providerUrl, String url) {
    String newUrl = url.replace(providerUrl, "").replace("/", "");

    // Elimina los ceros a la izquierda del número
    if (!newUrl.endsWith("final")) {
      newUrl = newUrl.replaceAll("-(0*)(\\d+)/?$", "/$2");
    } else {
      return newUrl.replaceAll("-(0*)(\\d+)-final", "/$2");
    }

    // Termina con -xx-2
    if (url.matches(".*-\\d{2}-2/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(\\d{2})-2/?$", "$1");
      try {
        int number = Integer.parseInt(numberPart) + 1;
        newUrl = url.replaceFirst("-\\d{2}-2/?$", "/" + number).replace(providerUrl, "");
        // log.info("1. Last URL: " + newUrl);
        return newUrl;
      } catch (NumberFormatException e) {
        log.warning("Error parsing number from URL: " + url);
      }

    // Termina con -xx-5
    } else if (url.matches(".*-\\d{1,2}-5/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(0*)(\\d{1,2})-5/?$", "$2");
      newUrl = url.replaceFirst("-\\d{1,2}-5/?$", "/" + numberPart + "-5").replace(providerUrl, "");
      // log.info("2. Last URL: " + newUrl);
      return newUrl;
    
    // Es un capítulo normal
    } else {
      // Elimina los ceros a la izquierda
      newUrl = url.replace(providerUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
      // log.info("3. Last URL: " + newUrl);
      return newUrl;
    }

    // Verifica si la URL termina con el patrón -xx-2
    if (url.matches(".*-\\d{2}-2/?$")) {
      // Extrae el número y lo incrementa
      String numberPart = url.replaceAll("^.*-(\\d{2})-2/?$", "$1");
      try {
        int number = Integer.parseInt(numberPart) + 1;
        newUrl = url.replaceFirst("-\\d{2}-2/?$", "/" + number);
        newUrl = newUrl.replace(providerUrl, "");
        // log.info("4. Last URL: " + newUrl);
        return newUrl;
      } catch (NumberFormatException e) {
        log.warning("Error parsing number from URL: " + url);
      }
    } else {
      // Si no termina con -xx-2, solo elimina los ceros a la izquierda
      newUrl = url.replace(providerUrl, "").replaceAll("-(0*)(\\d+)/?$", "/$2");
      // log.info("5. Last URL: " + newUrl);
      return newUrl;
    }

    // log.info("6. Last URL: " + newUrl);
    return newUrl;
  }

  // Calcula la fecha en la que se publicará el siguiente capítulo
  private String nextChapterDate(String name, String recivedDate) {
    String date = this.calcDaysToNextChapter(name, recivedDate);
    date = this.formatNextChapterDate(date);
    return date;
  }
  
  // Date: Calcula los días que faltan para el siguiente capítulo
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

  // Convierte las fechas de los animes que ya fueron subidos en un formato legible
  private String formatPastChapterDate(String dateText) {
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
  
  // Convierte las fechas de los animes que serán subidos en un formato legible
  private String formatNextChapterDate(String dateText) {
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
}

package xyz.kiridepapel.fraxianimebackend.services;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.ScheduleDTO;
import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.exceptions.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.interfaces.IJkScheduleService;
import xyz.kiridepapel.fraxianimebackend.utils.AnimeUtils;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@Service
public class JkScheduleServiceImpl implements IJkScheduleService {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${PROVIDER_1}")
  private String provider1;
  // Variables
  private Map<String, String> daysOfWeek;
  // Inyección de dependencias
  private final AnimeUtils animeUtils;
  private final CacheUtils cacheUtils;

  // Constructor
  public JkScheduleServiceImpl(AnimeUtils animeUtils, CacheUtils cacheUtils) {
    this.animeUtils = animeUtils;
    this.cacheUtils = cacheUtils;
  }

  // Inicialización
  @PostConstruct
  private void init() {
    this.daysOfWeek = Map.of(
      "MONDAY", "Lunes",
      "TUESDAY", "Martes",
      "WEDNESDAY", "Miercoles",
      "THURSDAY", "Jueves",
      "FRIDAY", "Viernes",
      "SATURDAY", "Sabado",
      "SUNDAY", "Domingo"
    );
  }

  @Cacheable(value = "schedule", key = "#keyName")
  public ScheduleDTO getSchedule(String keyName) {
    Document docJkAnime = AnimeUtils.tryConnectOrReturnNull((this.provider1 + "horario"), 1);
    if (docJkAnime == null) {
      throw new DataNotFoundException("No se pudo conectar con los proveedores");
    }

    // Mapa en base a la combinacion de los casos especiales 's' y 'n' (h) para AnimeLife
    Map<String, String> mapListTypeLf = new HashMap<>();
    List<SpecialCaseEntity> homeList = this.cacheUtils.getSpecialCases('s');
    homeList.addAll(this.cacheUtils.getSpecialCases('n'));
    homeList.forEach(hsce -> mapListTypeLf.put(hsce.getOriginal(), hsce.getMapped()));
    
    // Día actual
    LocalDate todayLD = DataUtils.getLocalDateTimeNow(this.isProduction).toLocalDate();
    int todayValue = todayLD.getDayOfWeek().getValue();
    todayValue = todayValue - 1; // Base 0
    // String todayName = this.daysOfWeek.get(todayLD.getDayOfWeek().toString());

    // Nombre de la temporada
    String title = docJkAnime.select(".horarioh2").text().replace("Horario Temporada ", "");
    String seasonName = title.split("\\(")[0].trim();
    List<String> seasonMonths = Arrays.asList(title.replace(")", "").split("\\(")[1].trim().split(" - "));
    
    // Objeto a devolver
    ScheduleDTO schedule = ScheduleDTO.builder()
      .todayValue(todayValue)
      .seasonName(seasonName)
      .seasonMonths(seasonMonths)
      .build();

    // Recorre los animes emitidos en el apartado 'Horario' de JkAnime   
    Elements elements = docJkAnime.select(".box.semana");
    elements.remove(elements.size() - 1); // Elimina el ultimo elemento (filtro)
    int i = 1;
    for (Element item : elements) {
      // Obtiene la fecha del día a recorrer en base a la resta del valor del día actual a la fecha actual, menos el valor del día a recorrer
      LocalDate dayLD = todayLD.minusDays(todayLD.getDayOfWeek().getValue() - i++);
      int dayValue = dayLD.getDayOfWeek().getValue(); // 1
      String dayName = this.daysOfWeek.get(dayLD.getDayOfWeek().toString());

      // Restar 7 días si el día a recorrer si es un día anterior a hoy
      String dateModified = DataUtils.parseDate(dayLD.toString(), "yyyy-MM-dd", "dd 'de' MMMM", 0);
      if (dayValue < todayValue) {
        dateModified = DataUtils.parseDate(dayLD.toString(), "yyyy-MM-dd", "dd 'de' MMMM", 7);
      }
      dateModified = dateModified.split(" ")[0] + " de " + DataUtils.firstUpper(dateModified.split(" ")[2]);

      // Lista de capítulos
      List<ChapterDataDTO> listChapters = new ArrayList<>();
      // Capítulos del día a recorrer
      for (Element subItem : item.select(".cajas .box")) {
        String name = subItem.select("a").first().select("h3").text();
        String imgUrl = subItem.select(".boxx img").attr("src");
        String url = subItem.select("a").first().attr("href").replace("/", "");
        String chapter = subItem.select(".last span").text();
        String timeStr = subItem.select(".last time").text().split(" ")[1];
        
        // Corregir la hora
        String[] listBadTimes = { "20", "21", "22", "23" };
        if (List.of(listBadTimes).contains(timeStr.substring(0, 2))) {
          Time time = Time.valueOf(timeStr);
          time.setTime(time.getTime() - 28800000); // -8 Horas
          timeStr = time.toString();
        }
        // Corregir casos especiales
        name = this.animeUtils.specialNameOrUrlCases(mapListTypeLf, name, 'k', "getSchedule()");
        url = this.animeUtils.specialNameOrUrlCases(mapListTypeLf, url, 'k', "getSchedule()");
        chapter = chapter.split(":")[1].trim();
        chapter = String.valueOf(Integer.parseInt(chapter) + 1);
        timeStr = timeStr.substring(0, timeStr.length() - 3);
        int timeHour = Integer.parseInt(timeStr.split(":")[0]);
        timeStr = (timeHour >= 0 && timeHour < 12) ? timeStr + " a.m." : timeStr + " p.m.";

        // Agregarlo a la lista solo si está en emisión
        if (subItem.select(".box .finished_anime").text().isEmpty()) {
          ChapterDataDTO anime = ChapterDataDTO.builder()
            .name(name)
            .url(url)
            .imgUrl(imgUrl)
            .chapter(chapter)
            .date(dateModified)
            .time(timeStr)
            .build();
        
          listChapters.add(anime);
        }
      }

      // Asignar la lista de capítulos al día correspondiente
      if (dayName.equals("Lunes")) schedule.setMonday(listChapters);
      else if (dayName.equals("Martes")) schedule.setTuesday(listChapters);
      else if (dayName.equals("Miercoles")) schedule.setWednesday(listChapters);
      else if (dayName.equals("Jueves")) schedule.setThursday(listChapters);
      else if (dayName.equals("Viernes")) schedule.setFriday(listChapters);
      else if (dayName.equals("Sabado")) schedule.setSaturday(listChapters);
      else if (dayName.equals("Domingo")) schedule.setSunday(listChapters);
    }

    return schedule;
  }
}

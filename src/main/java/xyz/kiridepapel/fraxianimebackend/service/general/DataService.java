package xyz.kiridepapel.fraxianimebackend.service.general;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.SpecialCaseDTO;
import xyz.kiridepapel.fraxianimebackend.entity.AnimeEntity;
import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.generic.AssignmentExportData;
import xyz.kiridepapel.fraxianimebackend.repository.AnimeRepository;
import xyz.kiridepapel.fraxianimebackend.repository.SpecialCaseRepository;
import xyz.kiridepapel.fraxianimebackend.service.anime.JkTopService;
import xyz.kiridepapel.fraxianimebackend.utils.CacheUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.utils.DatabaseUtils;

import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.*;

@Service
@Log
@SuppressWarnings("all")
public class DataService<T> {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  // Inyección de dependencias
  @Autowired
  private DatabaseUtils databaseUtils;
  @Autowired
  private CacheManager cacheManager;
  @Autowired
  private JkTopService jkTopService;
  // ? Repositorios (Agregar más según sea necesario)
  @Autowired
  private AnimeRepository animeRepository;
  @Autowired
  private SpecialCaseRepository specialCaseRepository;

  // * Export and Import data
  private AssignmentExportData assignData(String dataName, boolean searchList) {
    Class<?> clazz = null;
    List<? extends T> listRetrieved = null;

    // ! Agregar más casos según sea necesario
    switch (dataName) {
      case "translations":
        clazz = AnimeEntity.class;
        if (searchList) listRetrieved = (List<? extends T>) animeRepository.findAll();
        break;
      case "specialCases":
        clazz = SpecialCaseEntity.class;
        if (searchList) listRetrieved = (List<? extends T>) specialCaseRepository.findAll();
        break;
      default:
        throw new DataNotFoundException("El nombre de los datos a exportar no es válido");
    }

    return new AssignmentExportData(clazz, listRetrieved);
  }
  
  private void saveData(List<T> listRetrieved, String dataName) {
    // ! Agregar más casos según sea necesario
    switch (dataName) {
      case "translations":
        databaseUtils.resetTable("anime");
        animeRepository.saveAll((List<AnimeEntity>) listRetrieved);
        break;
      case "specialCases":
        databaseUtils.resetTable("special_case");
        specialCaseRepository.saveAll((List<SpecialCaseEntity>) listRetrieved);
        break;
      default:
        throw new DataNotFoundException("El nombre de los datos a importar no es válido");
    }
  }

  public byte[] exportExcel(String dataName) {
    // Define el nombre, tipo de clase y lista a exportar
    AssignmentExportData assignmentResultData = this.assignData(dataName, true);

    dataName = DataUtils.formatToNormalName(dataName);
    Class<?> clazz = assignmentResultData.getClazz();
    List<? extends T> listRetrieved = assignmentResultData.getListRetrieved();

    // Validaciones
    if (clazz == null || listRetrieved == null) {
      throw new DataNotFoundException("No se ha especificado la clase o los datos a exportar");
    }

    if (listRetrieved.size() > 0 && !listRetrieved.isEmpty()) {
      // Crear el archivo Excel
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet(dataName);

        // Obtener los campos de la clase a exportar
        Field[] fields = clazz.getDeclaredFields();

        // Crea dinámicamente la fila de encabezados
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < fields.length; i++) {
          headerRow.createCell(i).setCellValue(DataUtils.formatToNormalName(fields[i].getName()));
        }

        // Itera sobre la lista de datos
        int rowNum = 1;
        for (T item : listRetrieved) {
          Row row = sheet.createRow(rowNum++);

          // Iterar sobre los campos de la clase
          int cellNum = 0;
          for (Field field : fields) {
            field.setAccessible(true); // Permitir el acceso a campos privados
            Object value = field.get(item); // Obtiene el valor del campo

            // Si el valor del campo existe, asignarlo a la celda
            if (value != null) {
              row.createCell(cellNum++).setCellValue(value.toString());
            } else {
              // Si el valor del campo no existe, asignar una cadena vacía a la celda
              row.createCell(cellNum++).setCellValue("");
            }
          }
        }

        // Convertir el libro de trabajo a un array de bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);

        return outputStream.toByteArray();
      } catch (Exception e) {
        throw new DataNotFoundException("Ocurrió un error al exportar los datos");
      }
    } else {
      throw new DataNotFoundException("No hay datos para exportar");
    }
  }

  public void importExcel(String dataName, InputStream inputStream) {
    // Define el nombre, tipo de clase y lista a exportar
    AssignmentExportData assignmentResultData = this.assignData(dataName, false);

    Class<?> clazz = assignmentResultData.getClazz();
    List<T> listRetrieved = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = sheet.iterator();

      // Saltar la fila de encabezados
      if (iterator.hasNext()) {
        iterator.next();
      }

      // Obtener los campos de la clase a exportar
      Field[] fields = clazz.getDeclaredFields();

      // Iterar sobre las filas
      while (iterator.hasNext()) {
        Row currentRow = iterator.next();

        // Crea una instancia de la clase de forma dinámica
        T entity;
        try {
          entity = (T) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
            | InvocationTargetException e) {
          log.severe("No se pudo crear una nueva instancia de la entidad: " + e.getMessage());
          throw new DataNotFoundException("Ocurrió un error al importar los datos");
        }

        // Itera sobre los campos de la clase
        for (int i = 0; i < fields.length; i++) {
          if (i == 0) {
            continue;
          } else {
            try {
              Field field = fields[i];
              field.setAccessible(true); // Permitir el acceso a campos privados

              // Obtiene el tipo de datos del campo
              Class<?> fieldType = field.getType();

              // Obtiene la celda correspondiente a este campo
              Cell cell = currentRow.getCell(i);

              // Verifica si la celda no es nula y si el campo es una instancia de String
              if (cell != null && fieldType.equals(String.class)) {
                field.set(entity, cell.getStringCellValue());
              } else if (cell != null && fieldType.equals(Short.class)) {
                field.set(entity, (short) cell.getNumericCellValue());
              } else if (cell != null && fieldType.equals(Character.class)) {
                field.set(entity, cell.getStringCellValue().charAt(0));
              } else if (cell != null && fieldType.equals(Integer.class)) {
                field.set(entity, (int) cell.getNumericCellValue());
              } else if (cell != null && fieldType.equals(Double.class)) {
                field.set(entity, cell.getNumericCellValue());
              } else if (cell != null && fieldType.equals(Float.class)) {
                field.set(entity, (float) cell.getNumericCellValue());
              } else if (cell != null && fieldType.equals(Long.class)) {
                field.set(entity, (long) cell.getNumericCellValue());
              } else if (cell != null && fieldType.equals(Boolean.class)) {
                field.set(entity, cell.getBooleanCellValue());
              } else if (cell != null && fieldType.equals(Byte.class)) {
                field.set(entity, (byte) cell.getNumericCellValue());
              } else {
                log.severe("El tipo de datos de la celda no es válido para el campo: " + fields[i].getName());
              }

            } catch (Exception e) {
              log.severe("No se pudo asignar el valor a la celda: " + e.getMessage());
              throw new DataNotFoundException("La asignacion de celda no es válida para el campo '" + fields[i].getName() + "'");
            }
          }
        }

        // Verifica que todos los campos existan y no sea nulos
        int i = 0;
        for (Field requiredField : fields) {
          if (i != 0) {
            requiredField.setAccessible(true);
            try {
              Object value = requiredField.get(entity);
              if (value == null || value.toString().isEmpty()) {
                throw new DataNotFoundException("El campo obligatorio '" + requiredField.getName() + "' es nulo en una de las filas.");
              }
            } catch (IllegalAccessException e) {
              log.severe("No se pudo acceder al campo: " + e.getMessage());
              throw new DataNotFoundException("No se pudo acceder al campo obligatorio: " + requiredField.getName());
            }
          }
          i++;
        }

        // Agrega la entidad a la lista
        listRetrieved.add(entity);
      }

      if (listRetrieved.isEmpty()) {
        throw new DataNotFoundException("No hay datos para importar");
      } else {
        this.saveData(listRetrieved, dataName);
      }
    } catch (IOException e) {
      throw new DataNotFoundException("Ocurrió un error al importar los datos");
    }
  }

  // * Special cases
  public void newSpecialCase(SpecialCaseDTO data) {
    // Validaciones
    List<SpecialCaseEntity> listNewSpecialCases = new ArrayList<>();
    
    // Crear caso especial 1


    // Guardar caso especial
    // specialCaseRepository.saveAll(listNewSpecialCases);
  }
  
  // * Update cache
  public void updateTop() {
    log.info("-----------------------------------------------------");
    int actualYear = DataUtils.getLocalDateTimeNow(this.isProduction).getYear();
    CacheUtils.deleteFromCache(cacheManager, "pastYearsTop", null, true);
    
    // Lista de años disponibles
    List<String> yearList = new ArrayList<>();
    for (int i = 2020; i < actualYear; i++) {
      yearList.add(String.valueOf(i));
    }

    // Busca y guarda en caché el top de años pasados (para cada temporada de cada año)
    int counter = 0;
    for (String year : yearList) {
      for (String season : JkTopService.seasonNames) {
        if (!season.equals("actual")) {
          String key = year + "-" + season;
          log.info(String.format("%02d", counter++) + ". Guardando en cache: '" + key + "'");
          this.jkTopService.pastYearsCacheTop(year, season);
        }
      }
    }
    log.info("-----------------------------------------------------");
  }
}

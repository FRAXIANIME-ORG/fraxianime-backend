package xyz.kiridepapel.fraxianimebackend.service;

import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.entity.AnimeEntity;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.DataNotFound;
import xyz.kiridepapel.fraxianimebackend.repository.AnimeRepository;

@Service
@Log
public class TranslateService {
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${RAPIDAPI_KEY}")
  private String rapidApiKey;
  private String RAPIDAPI_HOST = "microsoft-translator-text.p.rapidapi.com";
  @Autowired
  private AnimeRepository animeRepository;

  public String translate(String name, String synopsis) {
    AnimeEntity anime = animeRepository.findByName(name);

    if (anime != null) {
      log.info("Se encontró el anime en la base de datos");
      return anime.getSynopsisEnglish();
    } else {
      if (isProduction == true) {
        try {
          synopsis = synopsis.replaceAll("\"", "+");
          String synopsisTranslated = this.translateWithMicrosoft(synopsis);
          synopsisTranslated = synopsisTranslated.replaceAll("\\+", "\"");
          animeRepository.save(new AnimeEntity(null, name, synopsisTranslated));
          return synopsisTranslated;
        } catch (Exception e) {
          log.info("Error al traducir: " + e.getMessage());
          return synopsis;
        }
      } else {
        return synopsis;
      }
    }
  }

  private String translateWithMicrosoft(String text) {
    String to = "en";
    String jsonBody = "[{\"Text\": \"" + text + "\"}]";

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://microsoft-translator-text.p.rapidapi.com/translate?to%5B0%5D=" + to
            + "&api-version=3.0&profanityAction=NoAction&textType=plain"))
        .header("content-type", "application/json")
        .header("X-RapidAPI-Key", rapidApiKey)
        .header("X-RapidAPI-Host", RAPIDAPI_HOST)
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    try {
      HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      // Obtener el texto traducido
      JSONArray jsonArray = new JSONArray(response.body());
      JSONObject jsonObject = jsonArray.getJSONObject(0);
      JSONObject translationObject = jsonObject.getJSONArray("translations").getJSONObject(0);
      String translatedText = translationObject.getString("text");

      return translatedText;
    } catch (Exception e) {
      return text;
    }
  }

  public byte[] databaseToExcel() {
    List<AnimeEntity> animes = animeRepository.findAll();

    if (animes.size() > 0 && !animes.isEmpty()) {
      // Crear el archivo Excel
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Translations");

        // Encabezados
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Nombre");
        headerRow.createCell(2).setCellValue("Sinopsis Ingles");

        // Datos
        int rowNum = 1;
        for (AnimeEntity anime : animes) {
          Row row = sheet.createRow(rowNum++);
          row.createCell(0).setCellValue(anime.getId().toString());
          row.createCell(1).setCellValue(anime.getName());
          row.createCell(2).setCellValue(anime.getSynopsisEnglish());
        }

        // Convertir el libro de trabajo a un array de bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);

        return outputStream.toByteArray();
      } catch (Exception e) {
        throw new DataNotFound("Ocurrió un error al exportar los datos");
      }
    } else {
      throw new DataNotFound("No hay animes para exportar");
    }
  }

  public String excelToDatabase(InputStream inputStream) {
    List<AnimeEntity> animeList = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = sheet.iterator();

      // Saltar la fila de encabezados
      if (iterator.hasNext()) {
        iterator.next();
      }

      // Iterar sobre las filas
      while (iterator.hasNext()) {
        Row currentRow = iterator.next();
        AnimeEntity anime = new AnimeEntity();
        Cell nameCell = currentRow.getCell(1);
        Cell synopsisEnglishCell = currentRow.getCell(2);

        // Verificar si las celdas no son nulas
        if (nameCell != null && synopsisEnglishCell != null) {
          anime.setName(nameCell.getStringCellValue());
          anime.setSynopsisEnglish(synopsisEnglishCell.getStringCellValue());
          animeList.add(anime);
        } else {
          log.info("Una de las celdas es nula en la fila " + currentRow.getRowNum());
        }
      }

      if (animeList.isEmpty()) {
        throw new DataNotFound("No hay datos para importar");
      } else {
        animeRepository.deleteAll();
        animeRepository.saveAll(animeList);
      }
    } catch (IOException e) {
      throw new DataNotFound("Ocurrió un error al importar los datos");
    }

    return "Datos importados correctamente";
  }
}

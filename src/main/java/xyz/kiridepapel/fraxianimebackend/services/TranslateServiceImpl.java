package xyz.kiridepapel.fraxianimebackend.services;

import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.entities.AnimeEntity;
import xyz.kiridepapel.fraxianimebackend.interfaces.ITranslateService;
import xyz.kiridepapel.fraxianimebackend.repositories.AnimeDaoRepository;

@Service
@Log
public class TranslateServiceImpl implements ITranslateService {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${RAPIDAPI_KEY}")
  private String rapidApiKey;
  // Variables
  private String RAPIDAPI_HOST = "microsoft-translator-text.p.rapidapi.com";
  // Inyección de dependencias
  private final AnimeDaoRepository animeRepository;

  // Constructor
  public TranslateServiceImpl(AnimeDaoRepository animeRepository) {
    this.animeRepository = animeRepository;
  }

  public String getTranslatedAndSave(String name, String text, String to) {
    AnimeEntity anime = animeRepository.findByName(name);

    if (anime != null) {
      return anime.getSynopsisEnglish();
    } else {
      if (this.isProduction) {
        try {
          text = text.replaceAll("\"", "+");
          String synopsisTranslated = this.translateWithMicrosoft(text, to);
          synopsisTranslated = synopsisTranslated.replaceAll("\\+", "\"");
          animeRepository.save(new AnimeEntity(null, name, synopsisTranslated));
          return synopsisTranslated;
        } catch (Exception e) {
          log.info("Error al traducir: " + e.getMessage());
          return text;
        }
      } else {
        return text;
      }
    }
  }
  
  public String getTranslated(String name, String from) {
    AnimeEntity anime = animeRepository.findByName(name);

    if (anime != null) {
      if (from.equals("en")) {
        return anime.getSynopsisEnglish();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private String translateWithMicrosoft(String text, String to) {
    // to = "en" -> inglés
    // to = "es" -> español
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

}

package xyz.kiridepapel.fraxianimebackend.service.general;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class MegaService {
  private static final String API = "https://g.api.mega.co.nz";
  private static final HttpClient httpClient = HttpClient.newHttpClient();

  public Boolean isValidMegaLink(String link) {
    if (!link.matches("https://mega\\.nz/(file|folder)/[\\s\\S]*#[\\s\\S]*")) {
      return false;
    }

    String[] parts = link.split("/");
    String type = parts[3];
    String id = parts[4].split("#")[0];

    JSONObject payload = new JSONObject();
    if (type.equals("folder")) {
      payload.put("a", "f").put("c", 1).put("r", 1).put("ca", 1);
    } else {
      payload.put("a", "g").put("p", id);
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API + "/cs?id=" + new Random().nextInt(1000000000) + "&n=" + id))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("[" + payload.toString() + "]"))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.body().startsWith("-")) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      return false;
    }
  }
}

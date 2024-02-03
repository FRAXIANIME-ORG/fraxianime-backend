package xyz.kiridepapel.fraxianimebackend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.repository.SpecialCaseRepository;

@SpringBootApplication
@EnableScheduling
public class FraxianimeBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(FraxianimeBackendApplication.class, args);
  }

  @Bean
  public CommandLineRunner initData(
    SpecialCaseRepository specialCaseRepository
  ) {
    return args -> {
      List<SpecialCaseEntity> specialCases = new ArrayList<>();

      if (specialCaseRepository.count() == 0) {
        // 1. No se encontró en JKAnime
        // 1. Sí se encontró en AnimeLife (anime y capítulos)
    
        // 2. Se quiere cambiar todo sobre un ánime en AnimeLife para que se alinie con el de JKAnime
    
        // ? Home AnimeLife -> Home MIO
        // h
        specialCases.add(new SpecialCaseEntity(null, 'h', "Solo Leveling", "Ore dake Level Up na Ken"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "solo-leveling", "ore-dake-level-up-na-ken"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        
        // ? Anime: MIO -> Jkanime (url)
        // 1: ("AnimeLife", "JKanime")
        // s
        specialCases.add(new SpecialCaseEntity(null, 'j', "maou-gakuin-no-futekigousha", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e"));
        specialCases.add(new SpecialCaseEntity(null, 'j', "maou-gakuin-no-futekigousha-2nd-season", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii"));
        
        // ? Anime: MIO -> AnimeLife (url)
        // 2: ("JKanime", "AnimeLife")
        // a
        specialCases.add(new SpecialCaseEntity(null, 'a', "ao-no-exorcist-shimane-illuminati-hen", "ao-no-exorcist-shin-series"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "captain-tsubasa-season-2-junior-youth-hen", "captain-tsubasa-junior-youth-hen"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        
        // ? Chapter: MIO -> AnimeLife (url)
        // 2: ("JKanime", "AnimeLife")
        // c
        specialCases.add(new SpecialCaseEntity(null, 'c', "ore-dake-level-up-na-ken", "solo-leveling"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        
        // ? Anime, Chapter: AnimeLife -> MIO (name)
        // 2: ("AnimeLife", "JKanime")
        // n
        specialCases.add(new SpecialCaseEntity(null, 'n', "Solo Leveling", "Ore dake Level Up na Ken"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2", "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Boku no Kokoro no Yabai Yatsu: Twi-Yaba", "Twi-Yaba"));
        
        // ? Chapter: MIO -> AnimeLife (name: buscar en lista de animes)
        // 2: ("JKanime", "AnimeLife")
        // l
        specialCases.add(new SpecialCaseEntity(null, 'l', "Ore dake Level Up na Ken", "Solo Leveling"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Chiyu Mahou no Machigatta Tsukaikata", "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season", "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Twi-Yaba", "Boku no Kokoro no Yabai Yatsu: Twi-Yaba"));
        
        // ? Search: AnimeLife -> MIO (url)
        // 2: ("AnimeLife", "JKanime")
        // s        
        specialCases.add(new SpecialCaseEntity(null, 's', "solo-leveling", "ore-dake-level-up-na-ken"));
        specialCases.add(new SpecialCaseEntity(null, 's', "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 's', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 's', "mushoku-tensei-isekai-ittara-honki-dasu-part-2", "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season"));
        specialCases.add(new SpecialCaseEntity(null, 's', "boku-no-kokoro-no-yabai-yatsu-twi-yaba", "twi-yaba"));

        // Save data
        specialCaseRepository.saveAll(specialCases);
      }
    };
  }

}

package xyz.kiridepapel.fraxianimebackend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
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
    public MessageSource messageSource() {
      ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
      messageSource.setBasename("classpath:/lang/lang");
      messageSource.setDefaultEncoding("UTF-8");
      messageSource.setUseCodeAsDefaultMessage(true);
      messageSource.setCacheSeconds(-1);
      return messageSource;
  }

  @Bean
  public CommandLineRunner initData(
    SpecialCaseRepository specialCaseRepository
  ) {
    return args -> {
      List<SpecialCaseEntity> specialCases = new ArrayList<>();

      if (specialCaseRepository.count() == 0) {
        // 1. Cambiar url de anime en JkAnime por la de AnimeLife
        // 2. Se quiere cambiar todo sobre un ánime en AnimeLife para que se alinie con el de JKAnime
        
        // ? Home AnimeLife -> Home MIO
        specialCases.add(new SpecialCaseEntity(null, 'h', "Solo Leveling", "Ore dake Level Up na Ken"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "solo-leveling", "ore-dake-level-up-na-ken"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"));
        specialCases.add(new SpecialCaseEntity(null, 'h', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        // specialCases.add(new SpecialCaseEntity(null, 'h', "", ""));
        
        // ? Anime: Busco final -> Encuentro en JkAnime
        // 1: ("AnimeLife", "JKanime")
        specialCases.add(new SpecialCaseEntity(null, 'j', "maou-gakuin-no-futekigousha", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e"));
        // specialCases.add(new SpecialCaseEntity(null, 'j', "", ""));

        // ? History: Mapeo arriba -> Encuentro en JkAnime (van los nombres y las urls que cambien)
        // 1: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'y', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"));
        // specialCases.add(new SpecialCaseEntity(null, 'y', "", ""));

        // ? Anime: Busco final -> Encuentro en AnimeLife
        // 2: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'a', "ao-no-exorcist-shimane-illuminati-hen", "ao-no-exorcist-shin-series"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "captain-tsubasa-season-2-junior-youth-hen", "captain-tsubasa-junior-youth-hen"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        // specialCases.add(new SpecialCaseEntity(null, 'a', "alice-to-therese-no-maboroshi-koujou", "maboroshi"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        // specialCases.add(new SpecialCaseEntity(null, 'a', "", ""));
        
        // ? Chapter: Busco final -> Encuentro en AnimeLife
        // 2: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'c', "ore-dake-level-up-na-ken", "solo-leveling"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "alice-to-therese-no-maboroshi-koujou", "maboroshi"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        // specialCases.add(new SpecialCaseEntity(null, 'c', "", ""));

        // ? Anime, Chapter: Encuentro en AnimeLife -> Obtengo final
        // 2: ("AnimeLife", "JKanime")
        specialCases.add(new SpecialCaseEntity(null, 'n', "Solo Leveling", "Ore dake Level Up na Ken"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2", "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Boku no Kokoro no Yabai Yatsu: Twi-Yaba", "Twi-Yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Maboroshi", "Alice to Therese no Maboroshi Koujou"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Maou Gakuin no Futekigousha 2nd Season", "Maou Gakuin no Futekigousha: Shijou Saikyou no Maou no Shiso, Tensei shite Shison-tachi no Gakkou e Kayou II"));
        // specialCases.add(new SpecialCaseEntity(null, 'n', "", ""));

        // ? Chapter: Mapeo arriba -> Encuentro en AnimeLife (lista de capitulos de un capítulo)
        // 2: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'l', "Ore dake Level Up na Ken", "Solo Leveling"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Chiyu Mahou no Machigatta Tsukaikata", "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season", "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Twi-Yaba", "Boku no Kokoro no Yabai Yatsu: Twi-Yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Alice to Therese no Maboroshi Koujou", "Maboroshi"));
        specialCases.add(new SpecialCaseEntity(null, 'l', "Maou Gakuin no Futekigousha: Shijou Saikyou no Maou no Shiso, Tensei shite Shison-tachi no Gakkou e Kayou II", "Maou Gakuin no Futekigousha 2nd Season"));
        // specialCases.add(new SpecialCaseEntity(null, 'l', "", ""));

        // ? Search: Busco animes -> Encuentro animes en AnimeLife
        // 2: ("AnimeLife", "JKanime")
        specialCases.add(new SpecialCaseEntity(null, 's', "solo-leveling", "ore-dake-level-up-na-ken"));
        specialCases.add(new SpecialCaseEntity(null, 's', "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 's', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 's', "mushoku-tensei-isekai-ittara-honki-dasu-part-2", "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season"));
        specialCases.add(new SpecialCaseEntity(null, 's', "boku-no-kokoro-no-yabai-yatsu-twi-yaba", "twi-yaba"));
        specialCases.add(new SpecialCaseEntity(null, 's', "maboroshi", "alice-to-therese-no-maboroshi-koujou"));
        specialCases.add(new SpecialCaseEntity(null, 's', "maou-gakuin-no-futekigousha-2nd-season", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii"));
        // specialCases.add(new SpecialCaseEntity(null, 's', "", ""));

        // Save data
        specialCaseRepository.saveAll(specialCases);
      }
    };
  }

}

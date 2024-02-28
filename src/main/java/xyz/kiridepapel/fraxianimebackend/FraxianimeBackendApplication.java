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
      // Gurda en la base de datos los Casos especiales
      List<SpecialCaseEntity> specialCases = new ArrayList<>();

      if (specialCaseRepository.count() == 0) {
        // ? 1. URL (Anime): Busco -> Encuentro en JkAnime
        specialCases.add(new SpecialCaseEntity(null, 'j', "maou-gakuin-no-futekigousha", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e"));
        specialCases.add(new SpecialCaseEntity(null, 'j', "the-daily-life-of-the-immortal-king-4ta-seanson", "the-daily-life-of-the-inmortal-king-4nd-season"));
        specialCases.add(new SpecialCaseEntity(null, 'j', "watamote", "watashi-ga-motenai-no-wa-dou-kangaetemo-omaera-ga-warui"));
        // specialCases.add(new SpecialCaseEntity(null, 'j', "", ""));

        // ? 1. Name && URL (Schedule JK && Historial JK): Encuentra en JkAnime > AnimeLife (Los que salen en Home, Historial y Schedule)
        specialCases.add(new SpecialCaseEntity(null, 'k', "The Daily Life of the Immortal King 4", "The Daily Life of the Immortal King 4ta Seanson"));
        specialCases.add(new SpecialCaseEntity(null, 'k', "the-daily-life-of-the-inmortal-king-4nd-season", "the-daily-life-of-the-immortal-king-4ta-seanson"));
        specialCases.add(new SpecialCaseEntity(null, 'k', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e", "maou-gakuin-no-futekigousha"));
        // specialCases.add(new SpecialCaseEntity(null, 'k', "", ""));

        // ? 2. Name (Anime && Chapter): Encuentra en AnimeLife -> JkAnime
        specialCases.add(new SpecialCaseEntity(null, 'n', "Solo Leveling", "Ore dake Level Up na Ken")); // name opcional
        specialCases.add(new SpecialCaseEntity(null, 'n', "Chiyu Mahou no Machigatta Tsukaikata: Senjou wo Kakeru Kaifuku Youin", "Chiyu Mahou no Machigatta Tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node", "Shin no Nakama ja Nai to Yuusha no Party wo Oidasareta node, Henkyou de Slow Life suru Koto ni Shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Mushoku Tensei: Isekai Ittara Honki Dasu Part 2", "Mushoku Tensei: Isekai Ittara Honki Dasu 2nd Season"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Boku no Kokoro no Yabai Yatsu: Twi-Yaba", "Twi-Yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Maou Gakuin no Futekigousha 2nd Season", "Maou Gakuin no Futekigousha: Shijou Saikyou no Maou no Shiso, Tensei shite Shison-tachi no Gakkou e Kayou II"));
        specialCases.add(new SpecialCaseEntity(null, 'n', "Karakai Jouzu no Takagi-san 1ra Temporada", "Karakai Jouzu no Takagi-san"));
        // specialCases.add(new SpecialCaseEntity(null, 'n', "", ""));
        
        // ? 2. URL (Anime): Mando -> Encuentra en AnimeLife
        specialCases.add(new SpecialCaseEntity(null, 'a', "ao-no-exorcist-shimane-illuminati-hen", "ao-no-exorcist-shin-series"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "captain-tsubasa-season-2-junior-youth-hen", "captain-tsubasa-junior-youth-hen"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'a', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        // specialCases.add(new SpecialCaseEntity(null, 'a', "", ""));
        
        // ? 2. URL (Chapter): Mando -> Encuentra en AnimeLife
        // 2: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'c', "ore-dake-level-up-na-ken", "solo-leveling"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "chiyu-mahou-no-machigatta-tsukaikata", "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season", "mushoku-tensei-isekai-ittara-honki-dasu-part-2"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "twi-yaba", "boku-no-kokoro-no-yabai-yatsu-twi-yaba"));
        specialCases.add(new SpecialCaseEntity(null, 'c', "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii", "maou-gakuin-no-futekigousha-2nd-season"));
        // specialCases.add(new SpecialCaseEntity(null, 'c', "", ""));

        // ? 2. URL (Search): Mando -> Encuentra en AnimeLife
        // 2: ("AnimeLife", "JKanime")
        specialCases.add(new SpecialCaseEntity(null, 's', "ao-no-exorcist-shin-series", "ao-no-exorcist-shimane-illuminati-hen"));
        specialCases.add(new SpecialCaseEntity(null, 's', "solo-leveling", "ore-dake-level-up-na-ken"));
        specialCases.add(new SpecialCaseEntity(null, 's', "chiyu-mahou-no-machigatta-tsukaikata-senjou-wo-kakeru-kaifuku-youin", "chiyu-mahou-no-machigatta-tsukaikata"));
        specialCases.add(new SpecialCaseEntity(null, 's', "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node", "shin-no-nakama-ja-nai-to-yuusha-no-party-wo-oidasareta-node-henkyou-de-slow-life-suru-koto-ni-shimashita"));
        specialCases.add(new SpecialCaseEntity(null, 's', "mushoku-tensei-isekai-ittara-honki-dasu-part-2", "mushoku-tensei-isekai-ittara-honki-dasu-2nd-season"));
        specialCases.add(new SpecialCaseEntity(null, 's', "boku-no-kokoro-no-yabai-yatsu-twi-yaba", "twi-yaba"));
        specialCases.add(new SpecialCaseEntity(null, 's', "maou-gakuin-no-futekigousha-2nd-season", "maou-gakuin-no-futekigousha-shijou-saikyou-no-maou-no-shiso-tensei-shite-shison-tachi-no-gakkou-e-kayou-ii"));
        specialCases.add(new SpecialCaseEntity(null, 's', "karakai-jouzu-no-takagi-san-1ra-temporada", "karakai-jouzu-no-takagi-san"));
        // specialCases.add(new SpecialCaseEntity(null, 's', "", ""));

        // ? 3. URL (Specific Chapter): Mando -> Encuentra en AnimeLife
        // 3: ("JKanime", "AnimeLife")
        specialCases.add(new SpecialCaseEntity(null, 'z', "karakai-jouzu-no-takagi-san_2", "karakai-jouzu-no-takagi-san-1ra-temporada"));
        specialCases.add(new SpecialCaseEntity(null, 'z', "karakai-jouzu-no-takagi-san_3", "karakai-jouzu-no-takagi-san-1ra-temporada"));
        // specialCases.add(new SpecialCaseEntity(null, 'z', "", ""));

        // Save data
        specialCaseRepository.saveAll(specialCases);
      }
    };
  }

}

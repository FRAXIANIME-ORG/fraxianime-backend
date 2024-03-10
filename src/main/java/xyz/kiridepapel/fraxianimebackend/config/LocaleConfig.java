package xyz.kiridepapel.fraxianimebackend.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

@Configuration
@SuppressWarnings("unused")
public class LocaleConfig implements WebMvcConfigurer {
  private final MessageSource messageSource;

  // Constructor
  public LocaleConfig(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  // Guarda en la sesión de cada usuario la configuración de idioma que elija
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver slr = new SessionLocaleResolver();
    slr.setDefaultLocale(Locale.US); // Idioma por defecto
    return slr;
  }

  // Cambia dinámicamente la configuración de idioma en función del parámetro "lang"
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
    lci.setParamName("lang");
    return lci;
  }

  // Agrega el interceptor creado anteriormente en el registro de interceptores de Spring MVC
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}

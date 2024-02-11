package xyz.kiridepapel.fraxianimebackend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
  private static final String[] ALLOWED_ORIGINS = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200"
  };

  @Autowired
  private ProtectedEntryPoint protectedEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(request -> {
          CorsConfiguration configuration = new CorsConfiguration();
          configuration.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
          configuration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name()));
          configuration.setAllowedHeaders(Arrays.asList("*"));
          // "Accept", "Accept-Charset", "Accept-Encoding", "Accept-Language",
          // "Accept-Datetime", "Authorization",
          return configuration;
        }))
        .exceptionHandling(exception -> exception.authenticationEntryPoint(protectedEntryPoint))
        .authorizeHttpRequests(authRequest -> {
          authRequest.requestMatchers("/api/v1/**").permitAll();
          authRequest.anyRequest().authenticated();
        })
        .build();
  }
}

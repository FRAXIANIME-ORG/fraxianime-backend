package xyz.kiridepapel.fraxianimebackend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
// import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityWebConfig {
  // Variables
  private static final String[] ALLOWED_ORIGINS = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200"
  };
  // Inyección de dependencias
  @Autowired
  private ProtectedEntryPoint protectedEntryPoint;
  // @Autowired
  // private JwtAuthenticationFilter jwtAuthenticationFilter;
  // @Autowired
  // private AuthenticationProvider authProvider;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(request -> {
          CorsConfiguration configuration = new CorsConfiguration();
          configuration.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
          configuration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name()));
          configuration.setAllowedHeaders(Arrays.asList("*"));
          return configuration;
        }))
        .exceptionHandling(exception -> exception.authenticationEntryPoint(protectedEntryPoint))
        .authorizeHttpRequests(authRequest -> {
          authRequest.requestMatchers("/api/v1/auth/**").permitAll();
          authRequest.requestMatchers("/api/v1/anime/**").permitAll();
          authRequest.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        // .sessionManagement(sessionManager -> { sessionManager
        //     .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // })
        // .authenticationProvider(authProvider)
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}

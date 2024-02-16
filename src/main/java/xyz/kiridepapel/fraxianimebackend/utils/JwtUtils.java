package xyz.kiridepapel.fraxianimebackend.utils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtUtils {
  @Value("${JWT_SECRET_KEY}")
  private String SECRET_KEY;

  @Value("${JWT_EXPIRATION_TIME}")
  private String TIME_EXPIRATION;

  private Set<String> memoryBackendBlacklistedTokens = new HashSet<>();

  // Obtener un Token de una Solicitud HTTP
  public String getTokenFromRequest(HttpServletRequest request) {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    } else {
      return null;
    }
  }

  // Generaciones
  public String genToken(UserDetails user) {
    Map<String, Object> extraClaims = new HashMap<>();
    return genToken(extraClaims, user);
  }

  private String genToken(Map<String, Object> extraClaims, UserDetails user) {
    return Jwts
        .builder()
        .setClaims(extraClaims)
        .setSubject(user.getUsername())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(TIME_EXPIRATION)))
        .signWith(genTokenSign(), SignatureAlgorithm.HS256)
        .compact();
  }

  private Key genTokenSign() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  // Validaciones
  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  private boolean isTokenExpired(String token) {
    return getExpiration(token).before(new Date());
  }

  public void addTokenToBlacklist(String token) {
    memoryBackendBlacklistedTokens.add(token);
  }

  public boolean isTokenBlacklisted(String token) {
    return memoryBackendBlacklistedTokens.contains(token);
  }

  // Claims del Token
  private Claims getAllClaims(String token) {
    return Jwts
        .parserBuilder()
        .setSigningKey(genTokenSign())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String getUsernameFromToken(String token) {
    return getClaim(token, Claims::getSubject);
  }

  private Date getExpiration(String token) {
    return getClaim(token, Claims::getExpiration);
  }
}

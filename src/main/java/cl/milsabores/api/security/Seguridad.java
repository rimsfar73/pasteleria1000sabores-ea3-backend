package cl.milsabores.api.security;

import cl.milsabores.api.usuario.repository.SesionRefreshRepository;
import cl.milsabores.api.usuario.entity.Usuario;
import cl.milsabores.api.usuario.repository.UsuarioRepository;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class Seguridad {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  // Las claves RSA persisten fuera del código y no se incluyen en el ZIP.
  @Bean
  KeyPair claves(@Value("${app.key-directory}") String directory) throws Exception {
    Path dir = Path.of(directory);
    Files.createDirectories(dir);
    Path priv = dir.resolve("private.der"), pub = dir.resolve("public.der");
    KeyFactory factory = KeyFactory.getInstance("RSA");
    if (Files.exists(priv) && Files.exists(pub))
      return new KeyPair(
          factory.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(pub))),
          factory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(priv))));
    if (Files.exists(priv) || Files.exists(pub))
      throw new IllegalStateException("Par de claves JWT incompleto.");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    Files.write(priv, pair.getPrivate().getEncoded(), StandardOpenOption.CREATE_NEW);
    Files.write(pub, pair.getPublic().getEncoded(), StandardOpenOption.CREATE_NEW);
    return pair;
  }

  @Bean
  JwtEncoder encoder(KeyPair pair) {
    var key =
        new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .keyID("ea3")
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder decoder(
      KeyPair pair,
      @Value("${app.issuer}") String issuer,
      @Value("${app.audience}") String audience) {
    var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
    var validators = JwtValidators.createDefaultWithIssuer(issuer);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            validators,
            jwt ->
                jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
    return decoder;
  }

  @Bean
  @Primary
  CorsConfigurationSource cors(@Value("${app.origin}") String origin) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origin));
    c.setAllowCredentials(true);
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  SecurityFilterChain filter(
      HttpSecurity http,
      UsuarioRepository usuarios,
      SesionRefreshRepository sesiones,
      CorsConfigurationSource corsSource,
      @Value("${app.cookie-secure}") boolean secure)
      throws Exception {
    var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookieCustomizer(c -> c.path("/").sameSite("Strict").secure(secure));
    // Se exige CSRF también para login/refresh/logout: usan cookie de sesión.
    http.cors(c -> c.configurationSource(corsSource))
        .csrf(
            c ->
                c.csrfTokenRepository(repository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/productos/**",
                        "/api/v1/categorias",
                        "/api/v1/ubicaciones",
                        "/api/v1/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/consultas")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.jwt(
                        jwt ->
                            jwt.jwtAuthenticationConverter(
                                token -> {
                                  Usuario u =
                                      usuarios
                                          .findById(token.getSubject())
                                          .filter(x -> x.activo)
                                          .orElseThrow(
                                              () ->
                                                  new org.springframework.security.oauth2.core
                                                      .OAuth2AuthenticationException(
                                                      "invalid_token"));
                                  String sid = token.getClaimAsString("sid");
                                  boolean vigente =
                                      sid != null
                                          && sesiones
                                              .findById(sid)
                                              .filter(
                                                  s ->
                                                      !s.revocada
                                                          && s.vence.isAfter(
                                                              java.time.Instant.now())
                                                          && s.usuario.id.equals(u.id))
                                              .isPresent();
                                  if (!vigente
                                      || u.tokenVersion
                                          != ((Number) token.getClaim("ver")).intValue())
                                    throw new org.springframework.security.oauth2.core
                                        .OAuth2AuthenticationException("invalid_token");
                                  return new JwtAuthenticationToken(
                                      token,
                                      List.of(new SimpleGrantedAuthority("ROLE_" + u.rol)),
                                      u.id);
                                }))
                    .authenticationEntryPoint((r, s, e) -> json(s, 401, "Debes iniciar sesión.")))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((r, s, x) -> json(s, 401, "Debes iniciar sesión."))
                    .accessDeniedHandler(
                        (r, s, x) -> json(s, 403, "Acceso denegado o token CSRF inválido.")));
    return http.build();
  }

  static void json(jakarta.servlet.http.HttpServletResponse response, int status, String mensaje)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"mensaje\":\"" + mensaje + "\",\"status\":" + status + "}");
  }
}

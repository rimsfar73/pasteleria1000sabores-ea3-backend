package cl.milsabores.api;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  final UsuarioRepository usuarios;
  final SesionRefreshRepository sesiones;
  final PasswordEncoder passwords;
  final JwtEncoder encoder;

  @Value("${app.issuer}")
  String issuer;

  @Value("${app.audience}")
  String audience;

  @Value("${app.access-minutes}")
  long minutes;

  @Value("${app.refresh-days}")
  long days;

  public AuthService(
      UsuarioRepository u, SesionRefreshRepository s, PasswordEncoder p, JwtEncoder e) {
    usuarios = u;
    sesiones = s;
    passwords = p;
    encoder = e;
  }

  public record Resultado(String accessToken, Dtos.UsuarioVista usuario, String refresh) {}

  public Resultado login(Dtos.Login d) {
    Usuario u =
        usuarios.findByCorreo(d.correo().trim().toLowerCase()).filter(x -> x.activo).orElse(null);
    if (u == null || !passwords.matches(d.clave(), u.claveHash))
      throw new ApiException(401, "Correo o contraseña incorrectos.");
    return nueva(u);
  }

  public Resultado renovar(String refresh) {
    if (refresh == null) throw new ApiException(401, "Sesión vencida.");
    SesionRefresh s =
        sesiones
            .bloquear(hash(refresh))
            .filter(x -> !x.revocada && x.vence.isAfter(Instant.now()) && x.usuario.activo)
            .orElseThrow(() -> new ApiException(401, "Sesión vencida."));
    s.revocada = true;
    return nueva(s.usuario);
  }

  public void salir(String refresh) {
    if (refresh != null) sesiones.bloquear(hash(refresh)).ifPresent(s -> s.revocada = true);
  }

  private Resultado nueva(Usuario u) {
    byte[] bytes = new byte[48];
    new SecureRandom().nextBytes(bytes);
    String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    SesionRefresh s = new SesionRefresh();
    s.hash = hash(refresh);
    s.usuario = u;
    s.vence = Instant.now().plus(Duration.ofDays(days));
    sesiones.save(s);
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(u.id)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(minutes * 60))
            .claim("rol", u.rol)
            .claim("ver", u.tokenVersion)
            .claim("sid", s.hash)
            .build();
    return new Resultado(
        encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(),
        Dtos.UsuarioVista.de(u),
        refresh);
  }

  static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
